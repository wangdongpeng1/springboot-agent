package com.agent.springbootrag.rag.rerank;

import com.agent.springbootrag.rag.rerank.client.BgeRerankClient;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 重排序
 * @description Spring AI目前没有“内置rerank”
 * 手动 调用 BGE / Jina / Cohere rerank API
 * 然后插入 CompositeDocumentRetriever → rerank → context
 */
@Service
@RequiredArgsConstructor
public class RerankService {

    private final BgeRerankClient rerankClient;

    public List<Document> rerank(String query, List<Document> merged) {

        if (merged.isEmpty()) {
            return merged;
        }

        return rerankClient.rerank(query, merged);
    }
}
