package com.agent.springbootsentinel.controller;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
public class TestController {

    @GetMapping(value = "/hello")
    @SentinelResource(value = "hello", blockHandler = "blockHandler", fallback = "fallback")
    public Mono<ResponseEntity<?>> hello() {
        return Mono.just(ResponseEntity.ok("Hello Sentinel"));
    }

    // 流控处理
    public Mono<ResponseEntity<?>> blockHandler(BlockException ex) {
        return Mono.just(ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(Map.of("error", "系统繁忙，请稍后再试")));
    }

    // 降级处理
    public Mono<ResponseEntity<?>> fallback(Throwable t) {
        return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "服务暂时不可用")));
    }

}
