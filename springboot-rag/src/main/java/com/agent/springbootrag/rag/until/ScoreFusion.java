package com.agent.springbootrag.rag.until;

import org.springframework.ai.document.Document;

/**
 * Fusion 计算（核心）
 */
public class ScoreFusion {

    // 权重（可以调优）
    private static final double ALPHA = 0.7; // vector
    private static final double BETA = 0.3;  // graph

    public static double calculate(Document doc) {

        double vectorScore = get(doc, "vectorScore");
        double graphScore = get(doc, "graphScore");

        return ALPHA * vectorScore + BETA * graphScore;
    }

    private static double get(Document doc, String key) {
        Object v = doc.getMetadata().get(key);
        return v == null ? 0.0 : Double.parseDouble(v.toString());
    }
}
