package com.agent.springbootrag.langgraph.graph;

import com.agent.springbootrag.langgraph.state.ChatState;
import org.bsc.langgraph4j.StateGraph;
import org.springframework.stereotype.Component;

import java.util.Map;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

/**
 * 定义 TestGraph 工作流
 */
@Component
public class TestGraph {

    /**
     * 构建 TestGraph 工作流
     *
     * @return StateGraph<ChatState> 实例
     * @throws Exception 异常
     */
    public StateGraph<ChatState> build() throws Exception {
        // 创建状态图，使用 ChatState::new 初始化 Graph State
        var graph = new StateGraph<>(ChatState::new);

        graph.addNode("hello", node_async(state -> {
            return Map.of(
                    ChatState.ANSWER,
                    "Hello World!"
            );
        }));

        graph.addEdge(StateGraph.START, "hello");
        graph.addEdge("hello", StateGraph.END);

        return graph;
    }
}
