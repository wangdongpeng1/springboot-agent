package com.agent.springboota2a.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/routing")
public class RoutingChatController {

    @Autowired
    @Lazy
    private ChatClient routingChatClient;

    @PostMapping("/chat")
    public ResponseEntity<String> chat(@RequestBody ChatRequest request) {

        if (request == null || request.message() == null || request.message().isBlank()) {
            return ResponseEntity.badRequest().body("message must not be blank");
        }

        String response = routingChatClient.prompt()
                .user(request.message())
                .call()
                .content();
        return ResponseEntity.ok(response);
    }

    public record ChatRequest(String message) {
    }
}
