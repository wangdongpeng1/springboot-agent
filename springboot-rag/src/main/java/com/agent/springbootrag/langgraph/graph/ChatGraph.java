package com.agent.springbootrag.langgraph.graph;

import com.agent.springbootrag.langgraph.state.ChatState;
import com.agent.springbootrag.langgraph.tool.WeatherTools;
import org.bsc.langgraph4j.StateGraph;
import org.bsc.langgraph4j.spring.ai.agentexecutor.AgentExecutor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.bsc.langgraph4j.GraphDefinition.END;
import static org.bsc.langgraph4j.GraphDefinition.START;
import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

/**
 * 定义 Graph 工作流
 */
@Component
public class ChatGraph {

    /**
     * 构建 ChatGraph 工作流
     *
     * @param chatModel  ChatModel 实例
     * @param chatClient ChatClient 实例
     * @return StateGraph<ChatState> 实例
     * @throws Exception 异常
     */
    public StateGraph<ChatState> build(ChatModel chatModel, ChatClient chatClient) throws Exception {
        // 定义工具列表
        List<ToolCallback> tools = Arrays.asList(ToolCallbacks.from(new WeatherTools()));
        // 创建状态图，使用 ChatState::new 初始化 Graph State
        var graph = new StateGraph<>(ChatState::new);
        // 定义ReAct Agent
        var agent = AgentExecutor.builder()
                .chatModel(chatModel)
                .tools(tools)
                .build()
                .compile();

        // =========================
        // ReAct Agent
        // =========================
        graph.addNode("agent", node_async(state -> {
            String question = state.question();
            // 调用 ReAct Agent
            var outputs = agent.stream(
                    Map.of(
                            "messages",
                            new UserMessage(question)
                    )
            );
            // 获取 Agent 最终回答
            String answer = outputs.stream()
                    .toList()
                    .getLast()
                    .state()
                    .lastMessage()
                    .map(Object::toString)
                    .orElse("");
            return Map.of(
                    ChatState.ANSWER,
                    answer
            );
        }));

        // =========================
        // Classify Intent
        // =========================
        graph.addNode("classify", node_async(state -> {
            String question = state.question();
            String intent = chatClient
                    .prompt()
                    .system("""
                            判断用户问题属于哪一类：
                                                
                            NORMAL
                            RAG
                            AGENT
                        
                            NORMAL：
                            普通闲聊、简单问答。
                        
                            RAG：
                            需要查询知识库才能回答的问题。
                        
                            AGENT：
                            需要调用工具或者执行任务的问题。
                        
                            只输出 NORMAL、RAG 或 AGENT。
                            """)
                    .user(question)
                    .call()
                    .content();
            intent = intent == null ? "" : intent.trim().toUpperCase();
            intent = switch (intent) {
                case "RAG" -> "RAG";
                case "AGENT" -> "AGENT";
                case "NORMAL" -> "NORMAL";
                default -> "NORMAL";
            };
            return Map.of(
                    ChatState.INTENT,
                    intent
            );
        }));

        // =========================
        // Question Answer
        // =========================
        graph.addNode("answer", node_async(state -> {
            String question = state.question();
            String answer = chatClient
                    .prompt()
                    .system("""
                            你是一个专业 AI 助手。
                            请根据用户问题回答。
                            """)
                    .user(question)
                    .call()
                    .content();
            return Map.of(
                    ChatState.ANSWER,
                    answer
            );
        }));

        // =========================
        // RAG
        // =========================
        graph.addNode("rag", node_async(state -> {

            String question = state.question();

            // TODO:

            return Map.of(
                    ChatState.ANSWER,
                    "这里是从 Milvus + Neo4j 获取的知识"
            );
        }));

        // ==========================
        // Workflow
        // ==========================
        graph
                .addEdge(START, "classify")
                .addConditionalEdges(
                        "classify",
                        state -> {
                            String intent = !"".equals(state.intent()) ? state.intent() : "NORMAL";
                            return CompletableFuture.completedFuture(intent);
                        },
                        Map.of(
                                "NORMAL", "answer",
                                "RAG", "rag",
                                "AGENT", "agent"
                        )
                )
                .addEdge("answer", END)
                .addEdge("rag", END)
                .addEdge("agent", END);

        return graph;
    }
}
