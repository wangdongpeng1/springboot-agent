package com.agent.springbootrag.rag.transformer;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.rag.preretrieval.query.transformation.CompressionQueryTransformer;
import org.springframework.ai.rag.preretrieval.query.transformation.QueryTransformer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 多轮压缩
 */
@Configuration
public class CompressionConfig {

    @Bean
    public QueryTransformer compression(ChatClient.Builder builder) {

        return CompressionQueryTransformer.builder()
                .chatClientBuilder(builder)
                .build();
    }
}
