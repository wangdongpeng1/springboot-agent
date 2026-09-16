package com.agent.springbootrag.langgraph.config;

import com.agent.springbootrag.langgraph.state.ChatState;
import org.bsc.langgraph4j.StateGraph;
import org.bsc.langgraph4j.studio.LangGraphStudioServer;
import org.bsc.langgraph4j.studio.springboot.LangGraphStudioConfig;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

/**
 * 定义 Studio 配置
 */
@Configuration
public class ChatLangGraphStudioConfig extends LangGraphStudioConfig {

    private final StateGraph<ChatState> chatWorkflow;

    private final StateGraph<ChatState> testWorkflow;

    public ChatLangGraphStudioConfig(StateGraph<ChatState> chatWorkflow,
                                     StateGraph<ChatState> testWorkflow) {
        this.chatWorkflow = chatWorkflow;
        this.testWorkflow = testWorkflow;
    }

    @Override
    public Map<String, LangGraphStudioServer.Instance> instanceMap() {

        var instance = LangGraphStudioServer.Instance.builder()
                        .title("My LangGraph")
                        .graph(testWorkflow)
                        .build();

        var chatInstance = LangGraphStudioServer.Instance.builder()
                .title("Chat Graph")
                .graph(chatWorkflow)
                .build();

        return Map.of(
                "default", instance,
                "chat", chatInstance
        );
    }

}
