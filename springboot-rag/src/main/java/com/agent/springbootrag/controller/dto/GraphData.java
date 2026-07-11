package com.agent.springbootrag.controller.dto;

import java.util.List;

/**
 * 图谱数据结构（供前端可视化）
 */
public record GraphData(
        List<GraphNode> nodes,
        List<GraphEdge> edges
) {
    public record GraphNode(String id, String name, String type) {}
    public record GraphEdge(String source, String target, String relation) {}
}
