package com.agent.springboota2a.config.agents;

import com.agent.springboota2a.tools.WeatherTools;
import io.a2a.server.agentexecution.AgentExecutor;
import io.a2a.spec.AgentCapabilities;
import io.a2a.spec.AgentCard;
import io.a2a.spec.AgentSkill;
import org.springaicommunity.a2a.server.executor.DefaultAgentExecutor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Create Agent
 */
@Configuration
public class WeatherAgentConfig {

    @Bean
    public AgentCard agentCard(@Value("${server.port:8080}") int port,
                               @Value("${server.servlet.context-path:/}") String contextPath) {
        // This AgentCard is automatically exposed at /.well-known/agent-card.json
        // Other agents discover this agent's capabilities through this endpoint
        return new AgentCard.Builder()
                .name("Weather Agent")
                .description("Provides weather information for cities")
                .url("http://localhost:" + port + contextPath + "/")
                .version("1.0.0")
                .capabilities(new AgentCapabilities.Builder().streaming(false).build())
                .defaultInputModes(List.of("text"))
                .defaultOutputModes(List.of("text"))
                .skills(List.of(new AgentSkill.Builder()
                        .id("weather_search")
                        .name("Search weather")
                        .description("Get temperature for any city")
                        .tags(List.of("weather"))
                        .examples(List.of("What's the weather in London?"))
                        .build()))
                .protocolVersion("0.3.0")
                .build();
    }

    @Bean
    public AgentExecutor agentExecutor(ChatClient.Builder chatClientBuilder, WeatherTools weatherTools) {

        ChatClient chatClient = chatClientBuilder.clone()
                .defaultSystem("You are a weather assistant. Use the temperature tool to answer questions.")
                .defaultTools(weatherTools)  // Register Spring AI tools
                .build();

        return new DefaultAgentExecutor(chatClient, (chat, ctx) -> {
            String userMessage = DefaultAgentExecutor.extractTextFromMessage(ctx.getMessage());
            return chat.prompt()
                    .user(userMessage)
                    .call()
                    .content();
        });
    }
}
