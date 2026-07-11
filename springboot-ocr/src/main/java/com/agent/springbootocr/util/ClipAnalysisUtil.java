package com.agent.springbootocr.util;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

/**
 * Chinese-CLIP 图文智能分析工具类
 *
 * 功能：计算图片与文本的语义相似度，判断图文是否匹配
 *
 * 使用场景：
 * 1. 从 MinerU 解析结果中提取图片和关联文本
 * 2. 调用 CLIP 服务计算图文相似度
 * 3. 根据阈值判断图文是否匹配
 *
 * @author springboot-ocr
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ClipAnalysisUtil {

    private final WebClient clipWebClient;

    @Value("${clip.analysis.match-threshold:0.5}")
    private double matchThreshold;

    /**
     * 分析单个图文对的匹配度
     *
     * 调用 POST /similarity_base64，请求体为 multipart form-data：
     * - image_base64：图片 Base64 编码
     * - texts：文本列表（此处传单条文本）
     *
     * @param imageBase64 图片 Base64 编码
     * @param text        关联文本（标题、周围正文等）
     * @return 相似度分数（0-1之间）
     */
    public Mono<Double> analyzeImageTextMatch(String imageBase64, String text) {
        log.info("开始分析图文匹配度，文本长度: {}", text.length());

        return clipWebClient.post()
                .uri("/similarity_base64")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData("image_base64", imageBase64)
                        .with("texts", List.of(text)))
                .retrieve()
                .bodyToMono(JsonNode.class)
                .map(response -> {
                    // 解析响应，提取相似度分数
                    JsonNode scores = response.get("scores");
                    if (scores != null && scores.isArray() && scores.size() > 0) {
                        double score = scores.get(0).asDouble();
                        log.info("图文匹配分析完成，相似度分数: {}", score);
                        return score;
                    }
                    log.warn("CLIP 响应格式异常，返回默认分数 0.0");
                    return 0.0;
                })
                .doOnError(err -> log.error("图文匹配分析失败: {}", err.getMessage(), err));
    }

    /**
     * 批量分析多图文对的匹配度
     *
     * 由于 /similarity_base64 每次只接受一张图片，批量场景需逐对调用。
     * 使用 concatMap 保证顺序，结果列表与输入一一对应。
     *
     * @param imageBase64List 图片 Base64 编码列表
     * @param textList        文本列表
     * @return 每对的相似度分数列表
     */
    public Mono<List<Double>> analyzeBatchImageTextMatch(
            List<String> imageBase64List, List<String> textList) {

        int size = Math.min(imageBase64List.size(), textList.size());
        log.info("开始批量分析图文匹配度，图片数: {}, 文本数: {}",
                imageBase64List.size(), textList.size());

        return Flux.range(0, size)
                .concatMap(i -> analyzeImageTextMatch(imageBase64List.get(i), textList.get(i)))
                .collectList()
                .doOnSuccess(scores -> log.info("批量图文匹配分析完成，共 {} 个分数", scores.size()))
                .doOnError(err -> log.error("批量图文匹配分析失败: {}", err.getMessage(), err));
    }

    /**
     * 判断图文是否匹配（基于阈值）
     *
     * @param score 相似度分数
     * @return true=匹配，false=不匹配
     */
    public boolean isMatch(double score) {
        return score >= matchThreshold;
    }

    /**
     * 分析图文匹配并返回结构化结果
     *
     * @param imageBase64 图片 Base64 编码
     * @param text        关联文本
     * @return 匹配结果（包含分数和是否匹配）
     */
    public Mono<ImageTextMatch> analyzeWithResult(String imageBase64, String text) {
        return analyzeImageTextMatch(imageBase64, text)
                .map(score -> new ImageTextMatch(0, 0, score, isMatch(score)));
    }

    /**
     * 批量分析并返回结构化结果列表
     *
     * @param imageBase64List 图片列表
     * @param textList        文本列表
     * @return 所有图文对的匹配结果
     */
    public Mono<List<ImageTextMatch>> analyzeBatchWithResult(
            List<String> imageBase64List, List<String> textList) {

        return analyzeBatchImageTextMatch(imageBase64List, textList)
                .map(scores -> {
                    List<ImageTextMatch> matches = new ArrayList<>();
                    for (int i = 0; i < scores.size(); i++) {
                        double score = scores.get(i);
                        matches.add(new ImageTextMatch(i, 0, score, isMatch(score)));
                    }
                    log.info("批量分析完成，共生成 {} 个匹配结果", matches.size());
                    return matches;
                });
    }

    /**
     * 图文匹配结果
     */
    public record ImageTextMatch(int imageIndex, int textIndex, double score, boolean isMatch) {
    }

}
