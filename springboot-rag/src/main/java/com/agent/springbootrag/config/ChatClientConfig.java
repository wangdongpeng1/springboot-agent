package com.agent.springbootrag.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * ChatClient（统一入口 + Memory）
 */
@Configuration
public class ChatClientConfig {

    @Bean
    public ChatClient chatClient(
            ChatModel chatModel,
            ChatMemory chatMemory
    ) {

        return ChatClient.builder(chatModel)
                .defaultAdvisors(
                        // Redis Memory（多轮对话）
                        MessageChatMemoryAdvisor.builder(chatMemory).build()
                )
                .build();
    }
}
