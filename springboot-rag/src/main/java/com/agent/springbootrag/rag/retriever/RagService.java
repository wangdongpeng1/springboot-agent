package com.agent.springbootrag.rag.retriever;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * 统一调用入口
 */

@Component
@RequiredArgsConstructor
@Slf4j
public class RagService {

    private final VectorStore vectorStore;

    /**
     * ReCall
     * @param question
     * @return
     */
    public List<Document> retriever(String question) {
        log.info("[Milvus] 输入查询文本: '{}'", question);

        List<Document> milvusDocs  =
                vectorStore.similaritySearch(
                        SearchRequest.builder()
                                .query(question)
                                .topK(5)
                                .similarityThreshold(0.4)
                                .build()
                );
        milvusDocs.forEach(d -> {
            double vectorScore = Optional.ofNullable(d.getScore())
                    .orElse(0.5);
            d.getMetadata().put("vectorScore", vectorScore);
        });

        log.info("[Milvus] 检索到 {} 条文档", milvusDocs.size());
        return milvusDocs;
    }
}
