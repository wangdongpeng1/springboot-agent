package com.agent.springbootrag.config;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.memory.repository.redis.RedisChatMemoryRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import redis.clients.jedis.RedisClient;

import java.time.Duration;

/**
 * Redis Chat Memory
 */
@Configuration
public class MemoryConfig {

    @Bean
    public RedisChatMemoryRepository redisChatMemoryRepository(
            @Value("${spring.data.redis.host:localhost}") String host,
            @Value("${spring.data.redis.port:6379}") int port,
            @Value("${spring.ai.chat.memory.redis.index-name:chat-memory-idx}") String indexName,
            @Value("${spring.ai.chat.memory.redis.key-prefix:chat-memory:}") String keyPrefix,
            @Value("${spring.ai.chat.memory.redis.time-to-live:}") Duration timeToLive,
            @Value("${spring.ai.chat.memory.redis.initialize-schema:true}") boolean initializeSchema
    ) {
        RedisClient jedisClient = RedisClient.builder()
                .hostAndPort(host, port)
                .build();

        RedisChatMemoryRepository.Builder builder = RedisChatMemoryRepository.builder()
                .jedisClient(jedisClient)
                .indexName(indexName)
                .keyPrefix(keyPrefix)
                .initializeSchema(initializeSchema);

        if (timeToLive != null && !timeToLive.isZero() && !timeToLive.isNegative()) {
            builder.timeToLive(timeToLive);
        }

        return builder.build();
    }

    @Bean
    public ChatMemory chatMemory(RedisChatMemoryRepository repo) {

        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(repo)
                .maxMessages(1000)
                .build();
    }
}
