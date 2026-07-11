package com.agent.springbootrag.rag.rerank.client;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class BgeRerankClient {

    private final RestTemplate restTemplate;

    @Value("${rag.rerank.url:http://localhost:8002/rerank}")
    private String rerankUrl;

    public List<Document> rerank(String query, List<Document> docs) {

        List<String> documents = docs.stream()
                .map(Document::getText)
                .toList();

        // Infinity API: {"query": "...", "documents": [...]}
        Map<String, Object> req = Map.of(
                "query", query,
                "documents", documents
        );

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(req);

        // Infinity API: {"results": [{"relevance_score": 0.99, "index": 0}, ...]}
        Map<String, Object> response = restTemplate.exchange(
                rerankUrl,
                HttpMethod.POST,
                entity,
                new ParameterizedTypeReference<Map<String, Object>>() {}
        ).getBody();

        // rebuild Document with rerank score (按 index 映射原文档)
        List<Document> reranked = new ArrayList<>();

        if (response != null && response.get("results") instanceof List<?> results) {
            for (Object item : results) {
                if (item instanceof Map<?, ?> r) {
                    int index = ((Number) r.get("index")).intValue();
                    double score = ((Number) r.get("relevance_score")).doubleValue();

                    if (index >= 0 && index < docs.size()) {
                        Document doc = docs.get(index);
                        doc.getMetadata().put("rerankScore", score);
                        reranked.add(doc);
                    }
                }
            }
        }

        return reranked;
    }
}
