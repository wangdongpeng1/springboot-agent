package com.agent.springbootrag.langgraph.state;

import org.bsc.langgraph4j.state.AgentState;

import java.util.Map;

/**
 * 定义 Graph State
 */
public class ChatState extends AgentState {

    public static final String INTENT = "intent";

    public static final String QUESTION = "question";

    public static final String ANSWER = "answer";

    public ChatState(Map<String, Object> initData) {
        super(initData);
    }

    public String intent() {
        return value(INTENT)
                .map(Object::toString)
                .orElse("");
    }

    public String question() {
        return value(QUESTION)
                .map(Object::toString)
                .orElse("");
    }

    public String answer() {
        return value(ANSWER)
                .map(Object::toString)
                .orElse("");
    }
}
