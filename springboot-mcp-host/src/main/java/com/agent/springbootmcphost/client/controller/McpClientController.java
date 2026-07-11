package com.agent.springbootmcphost.client.controller;

import com.agent.springbootmcphost.client.dto.McpExecuteParam;
import com.agent.springbootmcphost.client.dto.McpToolParam;
import com.agent.springbootmcphost.common.ErrorCode;
import com.agent.springbootmcphost.common.FlamesResponse;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.spec.McpClientTransport;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.mcp.client.webflux.transport.WebClientStreamableHttpTransport;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.EmitterProcessor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@RestController
@RequestMapping(value = "/flames/api/v1/mcp")
public class McpClientController {

    /**
     * 创建异步客户端Spec
     * @param url
     * @return
     */
    private McpClient.AsyncSpec createClient(String url) {
        // 获取baseUrl
        String baseUrl = UriComponentsBuilder.fromUriString(url)
                .build()
                .toUriString()
                .split("\\?")[0];
        // 获取authorization参数值
        String authorization = UriComponentsBuilder.fromUriString(url)
                .build()
                .getQueryParams()
                .getFirst("authorization");
        // 1. 创建传输层
        McpClientTransport transport = WebClientStreamableHttpTransport.builder(
                WebClient.builder()
                        .baseUrl(baseUrl)
                        .filter((request, next) -> {
                            URI newUri = UriComponentsBuilder.fromUri(request.url())
                                    .queryParam("authorization", authorization)
                                    .build(true)
                                    .toUri();
                            ClientRequest newRequest = ClientRequest.from(request)
                                    .url(newUri)
                                    .build();

                            return next.exchange(newRequest);
                        })).build();
        // 2. 创建异步客户端Spec
        McpClient.AsyncSpec asyncSpec = McpClient.async(transport);

        return asyncSpec;
    }

    /**
     * 获取工具列表
     *
     * @param param
     *  - {"url": "http://127.0.0.1:8082/api/v1?authorization=xxx"}
     * @return
     */
    @PostMapping("/tool")
    public Mono<FlamesResponse<Object>> mcpTool(@RequestBody McpToolParam param){
        // 初始化并列出工具
        return Mono.usingWhen(

                Mono.fromSupplier(() -> createClient(param.getUrl()).build()),

                client -> client.initialize()
                        .doOnSuccess(v -> log.info("初始化完成"))
                        .then(client.listTools())
                        .flatMap(tools -> {
                            log.info("可用工具 = {}", tools);
                            return Mono.<FlamesResponse<Object>>just(FlamesResponse.success(tools));
                        }),

                client -> client.closeGracefully()
                        .doOnSuccess(v -> log.info("MCP客户端关闭成功"))
                        .doOnError(e -> log.warn("关闭MCP客户端失败", e))
        ).onErrorResume(e -> {
            log.error("获取工具列表失败", e);
            return Mono.<FlamesResponse<Object>>just(FlamesResponse.error(ErrorCode.FAILURE, "获取工具列表失败"));
        });
    }

    @PostMapping("/execute")
    public Mono<FlamesResponse<Object>> mcpExecute(@RequestBody @Valid McpExecuteParam param) {
        // 参数校验
        if (param.getToolName() == null || param.getToolName().isEmpty()) {
            return Mono.<FlamesResponse<Object>>just(FlamesResponse.error(ErrorCode.FAILURE, "工具名称不能为空"));
        }
        if (param.getInput() == null || param.getInput().isEmpty()) {
            return Mono.<FlamesResponse<Object>>just(FlamesResponse.error(ErrorCode.FAILURE, "工具输入参数不能为空"));

        }
        // 创建工具调用请求
        McpSchema.CallToolRequest toolRequest = McpSchema.CallToolRequest.builder()
                .name(param.getToolName())
                .arguments(param.getInput())
                .build();

        // 初始化并调用工具
        return Mono.usingWhen(

                Mono.fromSupplier(() -> createClient(param.getUrl()).build()),

                client -> client.initialize()
                        .doOnSuccess(v -> log.info("初始化完成"))
                        .then(client.callTool(toolRequest))
                        .flatMap(result -> {
                            log.info("工具调用结果: " + result);
                            List<McpSchema.Content> content = result.content();
                            return Mono.<FlamesResponse<Object>>just(FlamesResponse.success((McpSchema.TextContent) content.get(0)));
                        }),

                client -> client.closeGracefully()
                        .doOnSuccess(v -> log.info("MCP客户端关闭成功"))
                        .doOnError(e -> log.warn("关闭MCP客户端失败", e))
        ).onErrorResume(e -> {
            log.error("执行工具失败: {}", e);
            return Mono.<FlamesResponse<Object>>just(FlamesResponse.error(ErrorCode.FAILURE, "执行工具失败: " + e));
        });
    }

    @PostMapping(value = "/stream/execute", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<FlamesResponse<Object>>> mcpStreamExecute(@RequestBody @Valid McpExecuteParam param) {
        // 参数校验 - 立即返回错误
        if (param.getToolName() == null || param.getToolName().isEmpty()) {
            return Flux.just(ServerSentEvent.<FlamesResponse<Object>>builder(
                            FlamesResponse.error(ErrorCode.FAILURE, "工具名称不能为空"))
                    .event("error")
                    .build());
        }
        if (param.getInput() == null || param.getInput().isEmpty()) {
            return Flux.just(ServerSentEvent.<FlamesResponse<Object>>builder(
                            FlamesResponse.error(ErrorCode.FAILURE, "工具输入参数不能为空"))
                    .event("error")
                    .build());
        }

        // 创建EmitterProcessor
        EmitterProcessor<ServerSentEvent<FlamesResponse<Object>>> processor = EmitterProcessor.create();
        FluxSink<ServerSentEvent<FlamesResponse<Object>>> sink = processor.sink();

        return Flux.usingWhen(

                Mono.fromSupplier(() -> createClient(param.getUrl())
                        .progressConsumer(notification -> {
                            sink.next(ServerSentEvent.<FlamesResponse<Object>>builder(FlamesResponse.success(notification))
                                    .event("progress")
                                    .build());
                            return Mono.empty();
                        })
                        .build()),

                client -> {
                    // 使用 AtomicReference 存储 token
                    AtomicReference<String> tokenRef = new AtomicReference<>();

                    return client.initialize()
                            .doOnSuccess(v -> log.info("初始化完成"))
                            .then(Mono.defer(() -> {
                                // 生成唯一的进度令牌
                                String progressToken = UUID.randomUUID().toString();
                                tokenRef.set(progressToken);  // 保存 token
                                log.info("生成进度令牌: {}", progressToken);
                                // 创建工具调用请求（包含进度令牌）
                                McpSchema.CallToolRequest toolRequest = McpSchema.CallToolRequest.builder()
                                        .name(param.getToolName())
                                        .arguments(param.getInput())
                                        .progressToken(progressToken)  // 设置进度令牌，用于接收进度通知
                                        .build();
                                return client.callTool(toolRequest);
                            }))
                            .flatMapMany(result -> {
                                sink.next(ServerSentEvent.<FlamesResponse<Object>>builder(
                                                FlamesResponse.success(result))
                                        .event("data")
                                        .build());
                                sink.complete();
                                return Flux.empty();
                            })
//                            .doOnComplete(() -> log.debug("流式传输完成，进度令牌: {}", tokenRef.get()))
                            .doOnError(e -> {
                                log.error("执行工具失败: {}", e);
                                sink.next(ServerSentEvent.<FlamesResponse<Object>>builder(
                                                FlamesResponse.error(ErrorCode.FAILURE, "执行工具失败: " + e))
                                        .event("error")
                                        .build());
                                sink.error(e);
                            })
                            .doFinally(s -> log.info("流式传输结束，signalType: {}, 进度令牌: {}", s, tokenRef.get()));
                },

                client -> client.closeGracefully()
                        .doOnSuccess(v -> log.info("MCP客户端关闭成功"))
                        .doOnError(e -> log.warn("关闭MCP客户端失败", e))
        )
        .doOnTerminate(() -> {
            if (!sink.isCancelled()) {
                sink.complete();
            }
        })
        .thenMany(processor)
        .onErrorResume(e -> {
            log.error("执行工具失败: {}", e);
            return Flux.just(ServerSentEvent.<FlamesResponse<Object>>builder(
                            FlamesResponse.error(ErrorCode.FAILURE, "执行工具失败: " + e))
                    .event("error")
                    .build());
        });
    }

    private String easyTextContent(List<McpSchema.Content> contentList) {
        assert contentList != null && contentList.size() == 1;
        McpSchema.TextContent textContent = (McpSchema.TextContent) contentList.get(0);
        return textContent.text();
    }

}
