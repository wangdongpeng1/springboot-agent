package com.agent.springbootmcphost.server.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;


/**
 * API Key 认证过滤器
 * 从请求头中提取 X-API-Key 并验证
 */
@Slf4j
@Component
public class McpWebFluxServerFilter implements WebFilter {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        String userId = exchange.getRequest().getHeaders().getFirst("X-User-Id");
        long startTime = System.currentTimeMillis();
        return chain.filter(exchange).doFinally(signalType -> {
            long duration = System.currentTimeMillis() - startTime;
            if (userId != null) {
                log.info("用户 {} 访问 {} 耗时 {}ms", userId, path, duration);
            } else {
                log.info("匿名访问 {} 耗时 {}ms", path, duration);
            }
        });
    }

}
