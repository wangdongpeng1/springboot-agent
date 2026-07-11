package com.agent.springbootocr.service;

import com.agent.springbootocr.util.ClipAnalysisUtil;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

/**
 * 图文智能分析服务
 *
 * 工作流程：
 * 1. 调用 MinerU 解析文档，提取图片和文本
 * 2. 使用 CLIP 分析图文匹配度
 * 3. 返回结构化分析结果
 *
 * @author springboot-ocr
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ImageTextAnalysisService {

    private final MinerUService minerUService;
    private final ClipAnalysisUtil clipAnalysisUtil;

    /**
     * 智能分析文档图文匹配关系
     *
     * 步骤1：调用 MinerU 解析上传的文档（{@link MinerUService#parseFile(FilePart)}）
     * 步骤2：从解析结果中提取图片节点及前后关联文本
     * 步骤3：调用 CLIP /similarity_base64 逐对计算图文相似度
     *
     * @param filePart 上传的文档文件（PDF / 图片等）
     * @return 图文匹配分析结果列表
     */
    public Mono<List<ClipAnalysisUtil.ImageTextMatch>> analyzeDocument(FilePart filePart) {
        log.info("开始智能分析文档图文匹配关系: {}", filePart.filename());

        // 步骤1：调用 MinerU 解析文档
        return minerUService.parseFile(filePart)
                // 步骤2 + 步骤3：提取图文对 → CLIP 分析
                .flatMap(this::analyzeFromParseResult)
                .doOnSuccess(results -> {
                    long matchCount = results.stream()
                            .filter(ClipAnalysisUtil.ImageTextMatch::isMatch)
                            .count();
                    log.info("文档 {} 图文匹配分析完成，共 {} 对，其中 {} 对匹配",
                            filePart.filename(), results.size(), matchCount);
                })
                .doOnError(err -> log.error("文档 {} 图文匹配分析失败: {}",
                        filePart.filename(), err.getMessage(), err));
    }

    /**
     * 基于已有 MinerU 解析结果分析图文匹配关系
     *
     * 适用于解析结果已缓存的场景，跳过步骤1直接进入提取 + 分析。
     *
     * @param parseResult MinerU 解析结果（含 content_list 数组）
     * @return 图文匹配分析结果列表
     */
    public Mono<List<ClipAnalysisUtil.ImageTextMatch>> analyzeFromParseResult(JsonNode parseResult) {
        log.info("基于已有解析结果开始图文匹配分析");

        // 步骤2：从 MinerU 结果中提取图片和关联文本
        return extractImageTextPairs(parseResult)
                .<List<ClipAnalysisUtil.ImageTextMatch>>flatMap(pairs -> {
                    if (pairs.isEmpty()) {
                        log.warn("未提取到有效的图文对");
                        return Mono.just(List.of());
                    }

                    log.info("提取到 {} 个图文对，开始 CLIP 分析", pairs.size());

                    // 步骤3：调用 CLIP 分析图文匹配度
                    List<String> images = new ArrayList<>();
                    List<String> texts = new ArrayList<>();

                    for (ImageTextPair pair : pairs) {
                        images.add(pair.imageBase64);
                        texts.add(pair.text);
                    }

                    return clipAnalysisUtil.analyzeBatchWithResult(images, texts);
                })
                .doOnError(err -> log.error("图文匹配分析失败: {}", err.getMessage(), err));
    }

    /**
     * 从 MinerU 解析结果中提取图文对
     *
     * 提取策略：
     * 1. 从 content_list 中提取图片节点
     * 2. 查找图片前后的文本节点（标题、正文）
     * 3. 组合成图文对
     *
     * @param parseResult MinerU 解析结果
     * @return 图文对列表
     */
    private Mono<List<ImageTextPair>> extractImageTextPairs(JsonNode parseResult) {
        return Mono.fromCallable(() -> {
            List<ImageTextPair> pairs = new ArrayList<>();

            JsonNode contentList = parseResult.get("content_list");
            if (contentList == null || !contentList.isArray()) {
                log.warn("解析结果中未找到 content_list");
                return pairs;
            }

            // 遍历 content_list，提取图片和关联文本
            for (int i = 0; i < contentList.size(); i++) {
                JsonNode node = contentList.get(i);
                String type = node.has("type") ? node.get("type").asText() : "";

                // 找到图片节点
                if ("image".equals(type)) {
                    String imageBase64 = extractImageBase64(node);
                    String relatedText = extractRelatedText(contentList, i);

                    if (imageBase64 != null && relatedText != null && !relatedText.isBlank()) {
                        pairs.add(new ImageTextPair(imageBase64, relatedText));
                        log.debug("提取图文对 #{}: 图片长度={}, 文本长度={}",
                                pairs.size(), imageBase64.length(), relatedText.length());
                    }
                }
            }

            return pairs;
        });
    }

    /**
     * 从图片节点提取 Base64 编码
     */
    private String extractImageBase64(JsonNode imageNode) {
        // 尝试从 img_path 或 img_base64 字段提取
        if (imageNode.has("img_base64")) {
            return imageNode.get("img_base64").asText();
        }
        if (imageNode.has("img_path")) {
            // 如果是文件路径，需要转换为 Base64（这里简化处理）
            log.warn("图片节点包含 img_path 而非 img_base64，需要额外处理");
            return null;
        }
        return null;
    }

    /**
     * 提取图片相关的文本（前后相邻的文本节点）
     */
    private String extractRelatedText(JsonNode contentList, int imageIndex) {
        StringBuilder textBuilder = new StringBuilder();

        // 向前查找（最多2个节点）
        for (int i = Math.max(0, imageIndex - 2); i < imageIndex; i++) {
            JsonNode node = contentList.get(i);
            String type = node.has("type") ? node.get("type").asText() : "";
            if ("text".equals(type) || "title".equals(type)) {
                String text = node.has("text") ? node.get("text").asText() : "";
                if (!text.isBlank()) {
                    textBuilder.append(text).append(" ");
                }
            }
        }

        // 向后查找（最多2个节点）
        for (int i = imageIndex + 1; i < Math.min(contentList.size(), imageIndex + 3); i++) {
            JsonNode node = contentList.get(i);
            String type = node.has("type") ? node.get("type").asText() : "";
            if ("text".equals(type) || "title".equals(type)) {
                String text = node.has("text") ? node.get("text").asText() : "";
                if (!text.isBlank()) {
                    textBuilder.append(text).append(" ");
                }
            }
        }

        return textBuilder.toString().trim();
    }

    /**
     * 图文对数据结构
     */
    private record ImageTextPair(String imageBase64, String text) {
    }
}
