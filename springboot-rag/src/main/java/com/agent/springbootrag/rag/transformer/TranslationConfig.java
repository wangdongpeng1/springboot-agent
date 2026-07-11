package com.agent.springbootrag.rag.transformer;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.rag.preretrieval.query.transformation.QueryTransformer;
import org.springframework.ai.rag.preretrieval.query.transformation.TranslationQueryTransformer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 中英文RAG
 */
@Configuration
public class TranslationConfig {

    @Bean
    public QueryTransformer translation(ChatClient.Builder builder) {

        return TranslationQueryTransformer.builder()
                .chatClientBuilder(builder)
                .targetLanguage("english")
                .build();
    }
}
