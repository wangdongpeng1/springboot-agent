package com.agent.springbootrag.controller.dto;

/**
 * 对话响应（包含 LLM 回答 + 图谱数据）
 */
public record ChatResponse(
        String answer,
        String conversationId,
        GraphData graph
) {}
