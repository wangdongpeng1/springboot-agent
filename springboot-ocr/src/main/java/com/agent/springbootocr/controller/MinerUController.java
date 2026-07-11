package com.agent.springbootocr.controller;

import com.agent.springbootocr.service.ImageTextAnalysisService;
import com.agent.springbootocr.service.MinerUService;
import com.agent.springbootocr.util.ClipAnalysisUtil;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * MinerU 文档解析接口（WebFlux 响应式）
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/ocr")
@CrossOrigin(origins = "*")
public class MinerUController {

    private final MinerUService minerUService;
    private final ImageTextAnalysisService imageTextAnalysisService;

    /**
     * 健康检查
     * GET /ocr/health
     */
    @GetMapping("/health")
    public Mono<JsonNode> health() {
        return minerUService.health();
    }

    /**
     * 同步解析 PDF
     * POST /api/doc/parse
     */
    @PostMapping(value = "/parse", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<JsonNode> parse(@RequestPart("file") FilePart file) {
        return minerUService.parseFile(file);
    }

    /**
     * 异步提交任务
     * POST /api/doc/submit
     */
    @PostMapping(value = "/submit", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<String> submit(@RequestPart("file") FilePart file) {
        return minerUService.submitTask(file);
    }

    /**
     * 查询任务结果
     * GET /api/doc/result/{taskId}
     */
    @GetMapping("/result/{taskId}")
    public Mono<Map> result(@PathVariable String taskId) {
        return minerUService.pollTaskResult(taskId);
    }

    /**
     * 智能图文匹配分析（MinerU 解析 + CLIP 相似度计算）
     * POST /api/ocr/analyze
     *
     * 完整工作流：上传文档 → MinerU 解析 → 提取图文对 → CLIP 分析匹配度
     */
    @PostMapping(value = "/analyze", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<List<ClipAnalysisUtil.ImageTextMatch>> analyze(@RequestPart("file") FilePart file) {
        return imageTextAnalysisService.analyzeDocument(file);
    }
}
