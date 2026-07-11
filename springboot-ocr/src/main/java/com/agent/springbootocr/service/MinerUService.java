package com.agent.springbootocr.service;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * MinerU 文档解析服务（响应式）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MinerUService {

    private final WebClient mineruWebClient;

    /**
     * 健康检查
     */
    public Mono<JsonNode> health() {
        return mineruWebClient.get()
                .uri("/health")
                .retrieve()
                .bodyToMono(JsonNode.class);
    }

    /**
     * 同步解析：返回 Markdown + 结构化内容
     * @param filePart
     * @return
     */
    public Mono<JsonNode> parseFile(FilePart filePart) {
        log.info("开始解析文件: {}", filePart.filename());

        return mineruWebClient.post()
                .uri("/file_parse")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData("files", filePart)
                        .with("return_md", "true")
                        .with("return_content_list", "true"))
                .retrieve()
                .bodyToMono(JsonNode.class)
                .doOnSuccess(result -> log.info("文件解析完成: {}", filePart.filename()))
                .doOnError(err -> log.error("文件解析失败: {}", filePart.filename(), err));
    }

    /**
     * 异步解析：提交任务 → 轮询结果（适合大文件）
     * @param filePart
     * @return
     */
    public Mono<String> submitTask(FilePart filePart) {
        log.info("提交异步解析任务: {}", filePart.filename());

        return mineruWebClient.post()
                .uri("/tasks")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData("files", filePart)
                        .with("return_md", "true"))
                .retrieve()
                .bodyToMono(Map.class)
                .map(resp -> String.valueOf(resp.get("task_id")))
                .doOnSuccess(taskId -> log.info("任务提交成功: {}, taskId={}", filePart.filename(), taskId))
                .doOnError(err -> log.error("任务提交失败: {}", filePart.filename(), err));
    }

    /**
     * 轮询任务结果
     * @param taskId
     * @return
     */
    public Mono<Map> pollTaskResult(String taskId) {
        log.info("轮询任务结果: taskId={}", taskId);

        return mineruWebClient.get()
                .uri("/tasks/{taskId}/result", taskId)
                .retrieve()
                .onStatus(
                        status -> status.value() == 404,
                        resp -> Mono.error(new RuntimeException("任务不存在: " + taskId))
                )
                .bodyToMono(Map.class)
                .doOnNext(result -> {
                    String status = (String) result.get("status");
                    if ("processing".equals(status)) {
                        log.info("任务处理中: taskId={}", taskId);
                    } else if ("completed".equals(status)) {
                        log.info("任务完成: taskId={}", taskId);
                    } else if ("failed".equals(status)) {
                        log.error("任务失败: taskId={}, message={}", taskId, result.get("message"));
                    }
                })
                .doOnError(err -> log.error("轮询失败: taskId={}", taskId, err));
    }

}
