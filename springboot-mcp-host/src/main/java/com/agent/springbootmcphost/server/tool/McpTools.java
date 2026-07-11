package com.agent.springbootmcphost.server.tool;

import com.agent.springbootmcphost.server.dto.PayloadSourceDto;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.agent.springbootmcphost.server.dto.DocumentParagraphDto;
import com.agent.springbootmcphost.server.utils.ToolUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.ai.mcp.annotation.context.McpAsyncRequestContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.io.IOException;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * 知识库问答工具
 * @author wangdp
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class McpTools {

    /**
     * WebClient
     */
    private final WebClient webClient;

    @Value("${mcp.rag.debug-url}")
    private String debugUrl;

    @McpTool(
            name = "retrieve_chunks",
            description = "检索知识库，返回原始分块列表，可选并返回调试日志"
    )
    public Mono<String> getRetrieveChunks(
            @McpToolParam(description = "检索问题", required = true) String message,
            @McpToolParam(description = "是否调试，默认 true", required = false) Boolean isDebug) {

        try {
            // 记录原始请求
            log.debug("原始的检索问题: {}", message);
            // 解析传入的JSON字符串
            JSONObject jsonObject = JSONObject.parseObject(message);
            // 从请求参数中获取traceId
            String traceId = Optional.ofNullable(jsonObject)
                    .map(obj -> obj.getJSONObject("header"))
                    .map(obj -> obj.getString("traceId"))
                    .orElseThrow(() -> new RuntimeException("未获取到traceId"));
            // 使用Optional链式调用处理payload
            JSONObject payload = Optional.ofNullable(jsonObject)
                    .map(obj -> obj.getJSONObject("payload"))
                    .orElseGet(() -> {
                        JSONObject newPayload = new JSONObject();
                        jsonObject.put("payload", newPayload);
                        return newPayload;
                    });
            // 设置DEBUG状态
            payload.put("debug", isDebug);
            // 获取修改后的请求字符串
            String modifiedRequest  = jsonObject.toJSONString();
            log.debug("修改后的检索问题: {}", modifiedRequest );

            return webClient.post()
                    .uri("/v1/chat")
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.TEXT_EVENT_STREAM)
                    .bodyValue(modifiedRequest)
                    .retrieve()
                    .bodyToMono(String.class)
                    .map(ToolUtils::extractJsonFromSse)
                    .flatMap(result -> {
                        // 解析响应
                        Optional<JSONObject> optional = Optional.ofNullable(result)
                                .map(JSONObject::parseObject);
                        // 解析传入的JSON字符串
                        List<DocumentParagraphDto> dtos = optional
                                .map(obj -> obj.getJSONObject("payload"))
                                .map(obj -> obj.getJSONArray("sources"))
                                .map(sources -> {
                                    return sources.stream()
                                            .map(JSONObject::from)
                                            .map(obj -> obj.getJSONArray("parts"))
                                            .filter(Objects::nonNull)
                                            .flatMap(array -> array.toJavaList(DocumentParagraphDto.class).stream())
                                            .collect(Collectors.toList());
                                })
                                .orElseThrow(() -> new RuntimeException("参数解析异常"));
                        // 获取调试日志
                        if (Boolean.TRUE.equals(isDebug)) {

                            log.debug("获取到traceId: {}, 延迟3秒后获取调试日志", traceId);

                            // 延迟3秒后获取调试日志
                            return Mono.delay(Duration.ofSeconds(3))
                                    .then(Mono.defer(() -> getDebugLogs(traceId)))
                                    .map(JSONObject::parseObject)
                                    .flatMap(debugLogs -> {
                                        // 返回结果
                                        Map<String, Object> resultMap = Map.of("chunks", dtos,
                                                "debugLogs", debugLogs);

                                        return Mono.just(JSON.toJSONString(resultMap));
                                    })
                                    .onErrorResume(error -> {
                                        log.error("获取调试日志失败, traceId: {}", traceId, error);
                                        return Mono.error(new RuntimeException("获取调试日志失败: " + error.getMessage()));
                                    });

                        } else {
                            // 返回结果
                            Map<String, Object> resultMap = Map.of("chunks", dtos,
                                    "debugLogs", "");

                            return Mono.just(JSON.toJSONString(resultMap));
                        }
                    })
                    .retryWhen(Retry.backoff(3, Duration.ofSeconds(1))  // 重试机制
                            .maxBackoff(Duration.ofSeconds(10))
                            .filter(throwable -> throwable instanceof IOException))
                    .onErrorResume(error -> {
                        log.error("检索知识库异常，执行降级处理", error);
                        Map<String, Object> resultMap = Map.of("chunks", Collections.emptyList(),
                                "debugLogs", null);

                        return Mono.just(JSON.toJSONString(resultMap));
                    });

        } catch (Exception e) {
            // 异常处理
            e.printStackTrace();
            return Mono.error(new RuntimeException("检索知识库异常: " + e.getMessage()));
        }
    }

    @Deprecated
    @McpTool(
            name = "retrieve_answer",
            description = "检索知识库并用大模型润色生成答案，可选并返回调试日志，支持流式返回"
    )
    public Flux<String> getRetrieveAnswer(
            McpAsyncRequestContext context,
            @McpToolParam(description = "检索问题", required = true) String message,
            @McpToolParam(description = "是否流式输出，默认 false", required = false) Boolean stream,
            @McpToolParam(description = "是否调试，默认 true", required = false) Boolean isDebug) {

        // 记录原始请求
        log.debug("检索问题: {}，是否流式输出: {}，是否调试：{}", message, stream, isDebug);

        // 全局traceId
        String globalTraceId;
        try {
            // 解析传入的JSON字符串
            JSONObject jsonObject = JSONObject.parseObject(message);
            // 从请求参数中获取traceId
            globalTraceId = Optional.ofNullable(jsonObject)
                    .map(obj -> obj.getJSONObject("header"))
                    .map(obj -> obj.getString("traceId"))
                    .orElseThrow(() -> new RuntimeException("未获取到traceId"));
            // 使用Optional链式调用处理payload
            JSONObject payload = Optional.ofNullable(jsonObject)
                    .map(obj -> obj.getJSONObject("payload"))
                    .orElseGet(() -> {
                        JSONObject newPayload = new JSONObject();
                        jsonObject.put("payload", newPayload);
                        return newPayload;
                    });
            // 设置DEBUG状态
            payload.put("debug", isDebug);
            // 获取修改后的请求字符串
            message  = jsonObject.toJSONString();
        } catch (Exception e) {
            // 异常处理
            e.printStackTrace();
            return Flux.error(new RuntimeException("参数解析失败: " + e.getMessage()));
        }

        // 是否流式输出
        if (Boolean.TRUE.equals(stream)) {

            Object progressToken = context.request().progressToken();
            // 添加一个AtomicReference来保存traceId
            AtomicReference<String> traceIdRef = new AtomicReference<>();

            return webClient.post()
                    .uri("/v1/chat")
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.TEXT_EVENT_STREAM)
                    .bodyValue(message)
                    .retrieve()
                    .bodyToFlux(String.class)
                    .map(ToolUtils::extractJsonFromSse)
                    .concatMap(result -> {
                        // 解析响应
                        Optional<JSONObject> optional = Optional.ofNullable(result)
                                .map(JSONObject::parseObject);
                        // 解析传入的JSON字符串
                        PayloadSourceDto dto = optional
                                .map(obj -> obj.getJSONObject("payload"))
                                .map(obj -> obj.toJavaObject(PayloadSourceDto.class))
                                .orElseThrow(() -> new RuntimeException("参数解析异常"));
                        // 如果是debug模式，保存traceId（性能可忽略不计）
                        if (Boolean.TRUE.equals(isDebug)) {
                            traceIdRef.set(globalTraceId);
                        }
                        // 发送进度信息
                        if (progressToken != null) {
                            JSONObject dtoJson = JSONObject.from(dto);
                            dtoJson.put("request_id", progressToken.toString());
                            return context.progress(p -> p.message(JSON.toJSONString(Map.of("event", "answer/chunk",
                                    "data", dtoJson.toJSONString()))));
                        }
                        return Flux.empty();
                    })
                    .thenMany(Flux.defer(() -> {
                        // 流式输出结束，发送结束事件
                        if (Boolean.TRUE.equals(isDebug)) {
                            String traceId = traceIdRef.get();

                            log.debug("获取到traceId: {}, 延迟3秒后获取调试日志", traceId);

                            // 延迟3秒后获取调试日志
                            return Mono.delay(Duration.ofSeconds(3))
                                    .then(Mono.defer(() -> getDebugLogs(traceId)))
                                    .map(JSONObject::parseObject)
                                    .flatMap(debugLogs -> {
                                        return Mono.just(JSON.toJSONString(Map.of("event", "answer/end",
                                                "data", Map.of("request_id", progressToken.toString(), "debugLogs", debugLogs))));
                                    })
                                    .onErrorResume(error -> {
                                        log.error("获取调试日志失败, traceId: {}", traceId, error);
                                        return Mono.error(new RuntimeException("获取调试日志失败: " + error.getMessage()));
                                    });
                        } else {
                            return Flux.just(JSON.toJSONString(Map.of("event", "answer/end",
                                    "data", Map.of("request_id", progressToken.toString(), "debugLogs", null))));
                        }
                    }))
                    .retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
                            .maxBackoff(Duration.ofSeconds(10))
                            .filter(throwable -> throwable instanceof IOException))
                    .onErrorResume(error -> {
                        log.error("检索知识库异常", error);
                        return Flux.just(JSON.toJSONString(Map.of("event", "answer/error",
                                "data", Map.of("request_id", progressToken.toString(),
                                        "error", error.getMessage()))));
                    });
        } else {

            return webClient.post()
                    .uri("/v1/chat")
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.TEXT_EVENT_STREAM)
                    .bodyValue(message)
                    .retrieve()
                    .bodyToFlux(String.class)
                    .map(ToolUtils::extractJsonFromSse)
                    .flatMap(result -> {
                        // 解析响应
                        Optional<JSONObject> optional = Optional.ofNullable(result)
                                .map(JSONObject::parseObject);
                        // 解析传入的JSON字符串
                        PayloadSourceDto dto = optional
                                .map(obj -> obj.getJSONObject("payload"))
                                .map(obj -> obj.toJavaObject(PayloadSourceDto.class))
                                .orElseThrow(() -> new RuntimeException("参数解析异常"));
                        // 获取调试日志
                        if (Boolean.TRUE.equals(isDebug)) {

                            log.debug("获取到traceId: {}, 延迟3秒后获取调试日志", globalTraceId);

                            // 延迟3秒后获取调试日志
                            return Mono.delay(Duration.ofSeconds(3))
                                    .then(Mono.defer(() -> getDebugLogs(globalTraceId)))
                                    .map(JSONObject::parseObject)
                                    .flatMap(debugLogs -> {
                                        // 返回结果
                                        Map<String, Object> resultMap = Map.of("answer", dto,
                                                "debugLogs", debugLogs);

                                        return Mono.just(JSON.toJSONString(resultMap));
                                    })
                                    .onErrorResume(error -> {
                                        log.error("获取调试日志失败, traceId: {}", globalTraceId, error);
                                        return Mono.error(new RuntimeException("获取调试日志失败: " + error.getMessage()));
                                    });
                        } else {
                            // 返回结果
                            Map<String, Object> resultMap = Map.of("answer", dto,
                                    "debugLogs", null);

                            return Flux.just(JSON.toJSONString(resultMap));
                        }
                    })
                    .retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
                            .maxBackoff(Duration.ofSeconds(10))
                            .filter(throwable -> throwable instanceof IOException))
                    .onErrorResume(error -> {
                        log.error("检索知识库异常，执行降级处理", error);
                        Map<String, Object> resultMap = Map.of("answer", null,
                                "debugLogs", null);

                        return Flux.just(JSON.toJSONString(resultMap));
                    });
        }
    }

    /**
     * 获取调试日志
     * @param traceId 追踪ID
     * @return 调试日志信息
     */
    private Mono<String> getDebugLogs(String traceId) {
        // 使用 mutate 动态设置 baseUrl
        WebClient client = webClient.mutate()
                .baseUrl(debugUrl)
                .build();

        return client.get()
                .uri("/v1/chat/debuglogs/{id}", traceId)
                .retrieve()
                .bodyToMono(String.class)
                .map(JSONObject::parseObject)
                .map(obj -> obj.getJSONObject("data"))
                .map(JSON::toJSONString)
                .flatMap(response -> {
                    return Mono.just(response);
                });
    }

}
