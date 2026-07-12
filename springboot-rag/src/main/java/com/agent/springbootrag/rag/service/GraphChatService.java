package com.agent.springbootrag.rag.service;

import com.agent.springbootrag.controller.dto.GraphData;
import com.agent.springbootrag.controller.dto.GraphData.GraphEdge;
import com.agent.springbootrag.controller.dto.GraphData.GraphNode;
import com.agent.springbootrag.rag.rerank.RerankService;
import com.agent.springbootrag.rag.retriever.HybridRagService;
import com.agent.springbootrag.rag.until.ScoreFusion;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.preretrieval.query.expansion.MultiQueryExpander;
import org.springframework.ai.rag.preretrieval.query.transformation.QueryTransformer;
import org.springframework.ai.rag.retrieval.join.ConcatenationDocumentJoiner;
import org.springframework.ai.rag.retrieval.join.DocumentJoiner;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import reactor.core.publisher.Flux;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 图谱增强对话服务（独立于 ChatService，提供前端可视化数据）
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GraphChatService {

    private final ChatClient chatClient;
    private final HybridRagService hybridRagService;
    private final @Qualifier("rewrite") QueryTransformer rewrite;
    private final MultiQueryExpander expander;
    private final RerankService rerankService;
    private final ChatMemory chatMemory;

    /**
     * 流式问答结果：图谱数据 + LLM token 流
     */
    public record StreamResult(GraphData graphData, Flux<String> tokenStream) {}

    /**
     * GraphRAG 主管线：检索 → 图谱提取 → 流式 LLM
     */
    public StreamResult askWithGraph(String question, String conversationId) {
        // 0. 指代消解
        String enrichedQuestion = enrichWithContext(question, conversationId);
        log.info("[GraphRAG] 原始问题: '{}', 增强后: '{}'", question, enrichedQuestion);

        // 1. query
        Query query = Query.builder().text(enrichedQuestion).build();

        // 2. rewrite
        Query rewritten = rewrite.transform(query);

        // 3. expand
        List<Query> expandedQueries = expander.expand(rewritten);

        // 4. retrieve
        Map<Query, List<List<Document>>> docsForQuery = new LinkedHashMap<>();
        List<Document> allNeo4jDocs = new ArrayList<>();
        for (Query q : expandedQueries) {
            List<List<Document>> pair = hybridRagService.retrieve(q.text());
            docsForQuery.put(q, pair);
            // 单独收集所有 Neo4j 原始结果（用于图谱可视化，不受 rerank/topK 过滤）
            if (pair.size() > 1) {
                allNeo4jDocs.addAll(pair.get(1));
            }
        }

        // 5. join
        DocumentJoiner joiner = new ConcatenationDocumentJoiner();
        List<Document> merged = joiner.join(docsForQuery);
        log.info("[GraphRAG] 检索结果: merged={} 条", merged.size());

        // 6. fusion score
        merged.forEach(d -> {
            double finalScore = ScoreFusion.calculate(d);
            d.getMetadata().put("finalScore", finalScore);
        });

        // 7. rerank
        List<Document> reranked = rerankService.rerank(rewritten.text(), merged);

        // 8. topK
        List<Document> topK = reranked.stream().limit(10).toList();

        // 9. 拆分语义/图谱文档
        List<Document> semanticDocs = topK.stream()
                .filter(d -> "milvus".equals(d.getMetadata().get("source")))
                .toList();
        List<Document> graphDocs = topK.stream()
                .filter(d -> "neo4j".equals(d.getMetadata().get("source")))
                .toList();
        log.info("[GraphRAG] 语义信息条数: {}, 图谱路径条数: {}", semanticDocs.size(), graphDocs.size());

        // 10. 构建上下文
        String semanticContext = buildSemanticContext(semanticDocs);
        String graphContext = buildGraphContext(graphDocs);

        // 11. Prompt
        PromptTemplate promptTemplate = new PromptTemplate("""
                你是化学品安全专家（EHS / SDS 领域）。

                【问题】
                {question}

                【语义信息（Vector Knowledge）】
                {semantic}

                【知识图谱路径（Graph Knowledge）】
                {graph}

                -----------------------------
                请输出：
                1. 风险说明
                2. 处理步骤
                3. 法规依据
                """);
        Prompt prompt = promptTemplate.create(Map.of(
                "question", rewritten.text(),
                "semantic", semanticContext,
                "graph", graphContext
        ));

        // 12. 流式输出 LLM
        Flux<String> tokenStream = chatClient.prompt(prompt)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                .stream()
                .content();

        // 13. 提取图谱数据（从原始 Neo4j 结果提取，不受 rerank/topK 截断）
        GraphData graphData = extractGraphData(allNeo4jDocs);

        return new StreamResult(graphData, tokenStream);
    }

    // ==================== 图谱数据提取 ====================

    /**
     * 从 Neo4j graphDocs 中提取节点和边
     * 解析格式："source=甲醇, relation=REQUIRES_PPE, target=化学安全防护眼镜"
     */
    private GraphData extractGraphData(List<Document> graphDocs) {
        Set<String> nodeIds = new LinkedHashSet<>();
        Set<String> edgeKeys = new LinkedHashSet<>();
        List<GraphNode> nodes = new ArrayList<>();
        List<GraphEdge> edges = new ArrayList<>();

        for (Document doc : graphDocs) {
            Map<String, String> triple = parseTriple(doc.getText());
            String source = triple.getOrDefault("source", "");
            String relation = triple.getOrDefault("relation", "");
            String target = triple.getOrDefault("target", "");

            if (source.isEmpty()) continue;

            if (nodeIds.add(source)) {
                nodes.add(new GraphNode(source, source, "chemical"));
            }
            if (!target.isEmpty() && nodeIds.add(target)) {
                nodes.add(new GraphNode(target, target, "attribute"));
            }
            // 边去重：source+relation+target 组合唯一
            if (!target.isEmpty()) {
                String edgeKey = source + "|" + relation + "|" + target;
                if (edgeKeys.add(edgeKey)) {
                    edges.add(new GraphEdge(source, target, relation));
                }
            }
        }
        return new GraphData(nodes, edges);
    }

    private Map<String, String> parseTriple(String text) {
        Map<String, String> result = new HashMap<>();
        for (String part : text.split(",\\s*")) {
            int eq = part.indexOf('=');
            if (eq > 0) {
                result.put(part.substring(0, eq).trim(), part.substring(eq + 1).trim());
            }
        }
        return result;
    }

    // ==================== 上下文构建 ====================

    private String buildSemanticContext(List<Document> docs) {
        return docs.stream()
                .collect(Collectors.toMap(Document::getText, this::getVectorScore, (a, b) -> a))
                .entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .limit(8)
                .map(e -> String.format("[%.3f] %s", e.getValue(), e.getKey()))
                .collect(Collectors.joining("\n"));
    }

    private String buildGraphContext(List<Document> docs) {
        return docs.stream()
                .collect(Collectors.toMap(Document::getText, this::getGraphScore, (a, b) -> a))
                .entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .limit(8)
                .map(e -> String.format("[%.3f] GRAPH_PATH: %s", e.getValue(), e.getKey()))
                .collect(Collectors.joining("\n"));
    }

    private double getVectorScore(Document d) {
        return Optional.ofNullable(d.getMetadata().get("vectorScore"))
                .map(Object::toString).map(Double::parseDouble).orElse(0.0);
    }

    private double getGraphScore(Document d) {
        return Optional.ofNullable(d.getMetadata().get("graphScore"))
                .map(Object::toString).map(Double::parseDouble).orElse(0.0);
    }

    // ==================== 指代消解 ====================

    private String enrichWithContext(String question, String conversationId) {
        try {
            List<Message> history = chatMemory.get(conversationId);
            if (history == null || history.isEmpty()) {
                return question;
            }
            String rawMessage = history.stream()
                    .filter(m -> m.getMessageType() == MessageType.USER)
                    .map(Message::getText)
                    .findFirst()
                    .orElse("");
            String previousQuestion = extractOriginalQuestion(rawMessage);
            if (previousQuestion.isBlank()) {
                return question;
            }
            return String.format("上一轮用户问的是关于%s的问题。当前问题：%s", previousQuestion, question);
        } catch (Exception e) {
            log.warn("[GraphRAG] 获取对话历史失败，使用原始问题: {}", e.getMessage());
            return question;
        }
    }

    private String extractOriginalQuestion(String rawMessage) {
        if (rawMessage == null || rawMessage.isBlank()) {
            return "";
        }
        int startIdx = rawMessage.indexOf("【问题】");
        int endIdx = rawMessage.indexOf("【语义信息（Vector Knowledge）】");
        if (startIdx >= 0 && endIdx > startIdx) {
            return rawMessage.substring(startIdx + "【问题】".length(), endIdx).trim();
        }
        return rawMessage.trim();
    }
}
