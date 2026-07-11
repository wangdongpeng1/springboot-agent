package com.agent.springbootrag.controller;

import com.agent.springbootrag.controller.dto.ChatResponse;
import com.agent.springbootrag.rag.service.ChatService;
import com.agent.springbootrag.rag.service.GraphChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * 对外接口
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/rag")
@CrossOrigin(origins = "http://localhost:5173")
public class ChatController {

    private final ChatService chatService;
    private final GraphChatService graphChatService;

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
     * 对话并返回图谱可视化数据
     */
    @GetMapping("/ask-with-graph")
    public ChatResponse askWithGraph(
            @RequestParam String question,
            @RequestParam(required = false) String conversationId
    ) {
        if (conversationId == null || conversationId.isBlank()) {
            conversationId = UUID.randomUUID().toString();
        }
        return graphChatService.askWithGraph(question, conversationId);
    }
}
