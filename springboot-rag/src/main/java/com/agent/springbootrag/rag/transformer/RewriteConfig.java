package com.agent.springbootrag.rag.transformer;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.rag.preretrieval.query.transformation.QueryTransformer;
import org.springframework.ai.rag.preretrieval.query.transformation.RewriteQueryTransformer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 问题改写
 */
@Configuration
public class RewriteConfig {

    @Bean
    public QueryTransformer rewrite(ChatClient.Builder builder) {

        return RewriteQueryTransformer.builder()
                .chatClientBuilder(builder)
                .build();
    }
}
