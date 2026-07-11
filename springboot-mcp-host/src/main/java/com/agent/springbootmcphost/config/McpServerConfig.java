package com.agent.springbootmcphost.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

@Configuration
public class McpServerConfig {

    @Value("${mcp.rag.url}")
    private String baseUrl;

    @Bean
    public WebClient webClient() {

        HttpClient httpClient = HttpClient.create()
                // 连接超时
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                // 响应超时
                .responseTimeout(Duration.ofMinutes(5))
                // 读写超时
                .doOnConnected(conn ->
                        conn.addHandlerLast(new ReadTimeoutHandler(-1))
                                .addHandlerLast(new WriteTimeoutHandler(30))
                );

        return WebClient.builder()
                .baseUrl(baseUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }

}
