package com.agent.springbootrag.langgraph.config;

import com.agent.springbootrag.langgraph.graph.ChatGraph;
import com.agent.springbootrag.langgraph.graph.TestGraph;
import com.agent.springbootrag.langgraph.state.ChatState;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.StateGraph;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 定义 Graph 配置
 */
@Configuration
public class ChatGraphConfig {

    /**
     * 定义 Chat Workflow
     */
    @Bean
    public StateGraph<ChatState> chatWorkflow(
            ChatGraph chatGraph,
            ChatModel chatModel,
            ChatClient chatClient
    ) throws Exception {

        return chatGraph.build(
                chatModel,
                chatClient
        );
    }

    /**
     * 编译 Chat Graph
     */
    @Bean
    public CompiledGraph<ChatState> chatGraph(
            StateGraph<ChatState> chatWorkflow
    ) throws Exception {
        return chatWorkflow.compile();
    }

    /**
     * 定义 Test Workflow
     */
    @Bean
    public StateGraph<ChatState> testWorkflow(
            TestGraph testGraph
    ) throws Exception {

        return testGraph.build();
    }

    /**
     * 编译 Test Graph
     */
    @Bean
    public CompiledGraph<ChatState> testGraph(
            StateGraph<ChatState> testWorkflow
    ) throws Exception {
        return testWorkflow.compile();
    }
}
