package com.agent.springboota2a.config;

import com.agent.springboota2a.remote.RemoteAgentConnections;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

@Configuration
public class HostAgentConfiguration {

    @Bean
    @Lazy
    public ChatClient routingChatClient(ChatClient.Builder chatClientBuilder,
                                        RemoteAgentConnections remoteAgentConnections) {

        String systemPrompt = """
           You coordinate tasks across specialized agents.
           Available agents:
           %s
           Use the sendMessage tool to delegate tasks to the appropriate agent.
           """.formatted(remoteAgentConnections.getAgentDescriptions());

        return chatClientBuilder
                .defaultSystem(systemPrompt)
                .defaultTools(remoteAgentConnections)  // Register as Spring AI tool
                .build();
    }
}
