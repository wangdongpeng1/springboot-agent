package com.agent.springbootrag.rag.retriever;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 混合 RAG（Milvus + Neo4j）
 */
@Component
@RequiredArgsConstructor
public class HybridRagService {

    private final RagService ragService;

    private final GraphRagService graphRagService;

    /**
     * ReCall
     * @param question
     * @return
     */
    public List<List<Document>> retrieve(String question) {
        // Milvus retrieve
        List<Document> milvusDocs  = ragService.retriever(question);
        milvusDocs.forEach(d -> {
            d.getMetadata().put("source", "milvus");
            d.getMetadata().put("type", "vector");
        });
        // Neo4j retrieve
        List<Document> graphDocs = graphRagService.retriever(question);
        graphDocs.forEach(d -> {
            d.getMetadata().put("source", "neo4j");;
            d.getMetadata().put("type", "graph");
        });
        // result merge
        return List.of(milvusDocs, graphDocs);
    }
}
