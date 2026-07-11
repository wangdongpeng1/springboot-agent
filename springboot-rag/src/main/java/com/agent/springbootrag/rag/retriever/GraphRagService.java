package com.agent.springbootrag.rag.retriever;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class GraphRagService {

    private final Neo4jClient neo4jClient;

    /**
     * ReCall
     * @param question
     * @return
     */
    public List<Document> retriever(String question) {
        log.info("[Neo4j] 输入查询文本: '{}'", question);

        String cypher = """
            MATCH (c:Chemical)-[r]->(n)
            WHERE $question CONTAINS c.name
            WITH c, r, n,
                 coalesce(n.name, n.type) AS targetName
            WHERE targetName IS NOT NULL
            RETURN 
                c.name AS source,
                type(r) AS relation,
                targetName AS target,
                count(*) AS weight
        """;

        List<Document> result = neo4jClient.query(cypher)
                .bind(question)
                .to("question")
                .fetch()
                .all()
                .stream()
                .map(this::toDocument)
                .toList();
        log.info("[Neo4j] 检索到 {} 条图谱路径", result.size());
        return result;
    }

    private Document toDocument(Map<String, Object> row) {

        // ✔ 图谱权重（关系强度 / 命中次数 / 可扩展算法）
        double graphScore = computeGraphScore(row);

        String text = String.format(
                "source=%s, relation=%s, target=%s",
                row.get("source"),
                row.get("relation"),
                row.get("target")
        );

        Map<String, Object> metadata = new HashMap<>();

        // ✔ 重点：统一字段名
        metadata.put("graphScore", graphScore);

        // 可选：原始权重
        metadata.put("weight", row.get("weight"));

        return Document.builder()
                .text(text)
                .metadata(metadata)
                .build();
    }

    private double computeGraphScore(Map<String, Object> row) {

        // ✔ 基础版本（后续可以升级 PageRank / NodeRank）
        Object weightObj = row.get("weight");

        double weight = weightObj == null ? 1.0 :
                Double.parseDouble(weightObj.toString());

        // ✔ 可以加规则增强
        return Math.log(1 + weight);
    }
}
