package com.agent.springbootrag.controller;

import com.agent.springbootrag.langgraph.state.ChatState;
import com.agent.springbootrag.rag.service.ChatService;
import com.agent.springbootrag.rag.service.GraphChatService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.CompiledGraph;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.UUID;

/**
 * 对外接口
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/rag")
@CrossOrigin(origins = "http://localhost:5173")
public class ChatController {

    private final ChatService chatService;
    private final GraphChatService graphChatService;
    private final ObjectMapper objectMapper;
    private final CompiledGraph<ChatState> chatGraph;

    /**
     * Agent-Hybrid Ask
     * @param question
     * @param conversationId
     * @return
     */
    @GetMapping("/ask")
    public String ask(
            @RequestParam String question,
            @RequestParam(required = false) String conversationId
    ) {
        if (conversationId == null || conversationId.isBlank()) {
            conversationId = UUID.randomUUID().toString();
        }
        return chatService.ask(question, conversationId);
    }

    /**
     * SSE 流式对话 + 图谱可视化
     * <p>事件类型：graph → token → done / error</p>
     */
    @GetMapping(value = "/ask-with-graph", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter askWithGraph(
            @RequestParam String question,
            @RequestParam(required = false) String conversationId
    ) {
        if (conversationId == null || conversationId.isBlank()) {
            conversationId = UUID.randomUUID().toString();
        }
        SseEmitter emitter = new SseEmitter(300_000L);
        String cid = conversationId;
        try {
            // 1. 检索 + 图谱提取 + 构建流式 token
            GraphChatService.StreamResult result = graphChatService.askWithGraph(question, cid);

            // 2. 发送图谱事件
            emitter.send(SseEmitter.event()
                    .name("graph")
                    .data(result.graphData()));
            log.info("[Stream] graph 事件已发送，开始订阅 token 流");

            // 3. 流式输出 LLM token
            result.tokenStream()
                    .doOnSubscribe(s -> log.info("[Stream] Flux 已订阅，等待 Ollama 响应"))
                    .doOnNext(t -> log.debug("[Stream] 收到 token: {}", t.length() > 20 ? t.substring(0,20) : t))
                    .doOnError(e -> log.error("[Stream] Flux onError", e))
                    .doOnComplete(() -> log.info("[Stream] Flux onComplete"))
                    .subscribe(
                    token -> {
                        try {
                            // JSON 编码防止换行破坏 SSE 事件边界
                            emitter.send(SseEmitter.event()
                                    .name("token")
                                    .data(objectMapper.writeValueAsString(token)));
                        } catch (Exception e) {
                            log.warn("[Stream] 发送 token 失败", e);
                            emitter.completeWithError(e);
                        }
                    },
                    error -> {
                        log.error("[Stream] LLM 流式输出失败", error);
                        try {
                            emitter.send(SseEmitter.event()
                                    .name("error")
                                    .data(error.getMessage()));
                        } catch (Exception ignored) {}
                        emitter.completeWithError(error);
                    },
                    () -> {
                        try {
                            emitter.send(SseEmitter.event()
                                    .name("done")
                                    .data("{\"conversationId\":\"" + cid + "\"}"));
                            emitter.complete();
                        } catch (Exception e) {
                            emitter.completeWithError(e);
                        }
                    }
            );
        } catch (Exception e) {
            log.error("[Stream] 检索阶段失败", e);
            try {
                emitter.send(SseEmitter.event().name("error").data(e.getMessage()));
                emitter.completeWithError(e);
            } catch (Exception ignored) {}
        }
        return emitter;
    }

    @PostMapping
    public String chat(@RequestParam String question) throws Exception {
        Map<String, Object> input = Map.of(
                ChatState.QUESTION,
                question
        );

        var outputs = chatGraph.stream(input)
                .stream()
                .peek(output -> System.out.println("node = " + output.node()))
                .toList();

        return outputs.getLast()
                .state()
                .answer();
    }
}
