package com.agent.springbootrag.rag.expander;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.rag.preretrieval.query.expansion.MultiQueryExpander;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 召回增强
 */
@Configuration
public class MultiQueryExpanderConfig {

    @Bean
    public MultiQueryExpander expander(ChatClient.Builder builder) {

        return MultiQueryExpander.builder()
                .chatClientBuilder(builder)
                .numberOfQueries(3)
                .includeOriginal(true)
                .build();
    }
}
