package com.agent.springbootrag.rag.service;

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

import java.util.*;
import java.util.stream.Collectors;

/**
 * 混合 RAG（Milvus + Neo4j）
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {

    private final ChatClient chatClient;

    private final HybridRagService hybridRagService;

    private final @Qualifier("rewrite") QueryTransformer rewrite;

    private final MultiQueryExpander expander;

    private final RerankService rerankService;

    private final ChatMemory chatMemory;

    public String ask(String question, String conversationId) {
        // 0. 从对话记忆中获取历史上下文，用于指代消解
        String enrichedQuestion = enrichWithContext(question, conversationId);
        log.info("[RAG] 原始问题: '{}', 增强后: '{}'", question, enrichedQuestion);
        // 1. query
        Query query = Query.builder()
                .text(enrichedQuestion)
                .build();
        // 2. rewrite
        Query rewritten =
                rewrite.transform(query);
        // 3. expand（增强召回）
        // NOTE: MultiQueryExpander 扩展暂时因 Ollama 模型能力限制无法生效，但不影响核心 RAG 流程
        // （rewrite + 单查询检索 + rerank 仍然正常工作）。后续如果换用更强的模型（如 qwen2.5-14b 或接入云端 API），扩展功能会自动生效。
        List<Query> expandedQueries =
                expander.expand(rewritten);
        // 4. retrieve
        Map<Query, List<List<Document>>> docsForQuery = new LinkedHashMap<>();
        for (Query q : expandedQueries) {
            // hybrid rag
            List<List<Document>> docs = hybridRagService.retrieve(q.text());
            // result merge
            docsForQuery.put(
                    q,
                    docs);
        }
        // 5. join（官方方式）
        DocumentJoiner joiner = new ConcatenationDocumentJoiner();
        List<Document> merged = joiner.join(docsForQuery);
        log.info("[RAG] 检索结果: merged={} 条", merged.size());
        // 6. fusion score（粗排）
        merged.forEach(d -> {
            double finalScore = ScoreFusion.calculate(d);
            d.getMetadata().put("finalScore", finalScore);
        });
        // 7. rerank（query-aware精排）
        List<Document> reranked =
                rerankService.rerank(rewritten.text(), merged);
        // 8. topK
        List<Document> topK = reranked.stream()
                .limit(10)
                .toList();
        // 9. Hybrid context build
        HybridResult result = buildHybridContext(topK);
        log.info("[RAG] 语义信息条数: {}, 图谱路径条数: {}",
                result.semanticContext().isEmpty() ? 0 : result.semanticContext().split("\n").length,
                result.graphContext().isEmpty() ? 0 : result.graphContext().split("\n").length);
        // 10. Prompt
        PromptTemplate promptTemplate = buildPromptTemplate();
        Prompt prompt = promptTemplate.create(Map.of(
                "question", rewritten.text(),
                "semantic", result.semanticContext(),
                "graph", result.graphContext()
        ));
        // 11. LLM
        return chatClient.prompt(prompt)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                .call()
                .content();
    }

    /**
     * RAG系统的“最后一公里”，决定LLM理解质量，而不是检索质量
     * @param topK
     * @return
     */
    private HybridResult buildHybridContext(List<Document> topK) {
        // 1. 拆分
        List<Document> semanticDocs = topK.stream()
                .filter(d -> "milvus".equals(d.getMetadata().get("source")))
                .toList();

        List<Document> graphDocs = topK.stream()
                .filter(d -> "neo4j".equals(d.getMetadata().get("source")))
                .toList();

        // 2. semantic context（vector）
        String semantic = buildSemanticContext(semanticDocs);

        // 3. graph context（path format）
        String graph = buildGraphContext(graphDocs);

        return new HybridResult(semantic, graph);
    }

    /**
     * Token-based keyword extraction（轻量规则抽取）
     * @param docs
     * @return
     */
    @Deprecated
    private String buildKeywords(List<Document> docs) {

        return docs.stream()
                .map(Document::getText)
                .flatMap(t -> Arrays.stream(t.split("[\\s,，;；\\-→]+")))
                .filter(w -> w.length() > 1)
                .distinct()
                .limit(15)
                .collect(Collectors.joining(", "));
    }

    private String buildSemanticContext(List<Document> docs) {

        return docs.stream()
                .collect(Collectors.toMap(
                        Document::getText,
                        d -> getVectorScore(d),
                        (a, b) -> a
                ))
                .entrySet()
                .stream()
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .limit(8)
                .map(e -> String.format("[%.3f] %s", e.getValue(), e.getKey()))
                .collect(Collectors.joining("\n"));
    }

    private String buildGraphContext(List<Document> docs) {

        return docs.stream()
                .collect(Collectors.toMap(
                        Document::getText,
                        d -> getGraphScore(d),
                        (a, b) -> a
                ))
                .entrySet()
                .stream()
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .limit(8)
                .map(e -> String.format("[%.3f] GRAPH_PATH: %s", e.getValue(), e.getKey()))
                .collect(Collectors.joining("\n"));
    }

    private double getVectorScore(Document d) {
        return Optional.ofNullable(d.getMetadata().get("vectorScore"))
                .map(Object::toString)
                .map(Double::parseDouble)
                .orElse(0.0);
    }

    private double getGraphScore(Document d) {
        return Optional.ofNullable(d.getMetadata().get("graphScore"))
                .map(Object::toString)
                .map(Double::parseDouble)
                .orElse(0.0);
    }

    private PromptTemplate buildPromptTemplate() {

        return new PromptTemplate("""
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
    }

    public record HybridResult(String semanticContext, String graphContext) {}

    /**
     * 将对话历史注入当前问题，解决指代消解问题（如“它”“这个”）
     */
    private String enrichWithContext(String question, String conversationId) {
        try {
            List<Message> history = chatMemory.get(conversationId);
            if (history == null || history.isEmpty()) {
                return question;
            }
            // 取第一轮用户消息，并提取原始问题（排除 prompt 模板内容）
            String rawMessage = history.stream()
                    .filter(m -> m.getMessageType() == MessageType.USER)
                    .map(Message::getText)
                    .findFirst()
                    .orElse("");
            String previousQuestion = extractOriginalQuestion(rawMessage);
            if (previousQuestion.isBlank()) {
                return question;
            }
            return String.format("上一轮用户问的是关于“%s”的问题。当前问题：%s", previousQuestion, question);
        } catch (Exception e) {
            log.warn("[RAG] 获取对话历史失败，使用原始问题: {}", e.getMessage());
            return question;
        }
    }

    /**
     * 从存储的 prompt 中提取原始用户问题
     * MessageChatMemoryAdvisor 存储的是完整的 prompt（含 RAG 上下文），
     * 原始问题位于 【问题】 和 【语义信息（Vector Knowledge）】 之间
     */
    private String extractOriginalQuestion(String rawMessage) {
        if (rawMessage == null || rawMessage.isBlank()) {
            return "";
        }
        // 尝试从 prompt 模板中提取
        int startIdx = rawMessage.indexOf("【问题】");
        int endIdx = rawMessage.indexOf("【语义信息（Vector Knowledge）】");
        if (startIdx >= 0 && endIdx > startIdx) {
            return rawMessage.substring(startIdx + "【问题】".length(), endIdx).trim();
        }
        // 非模板格式，直接返回
        return rawMessage.trim();
    }

}
