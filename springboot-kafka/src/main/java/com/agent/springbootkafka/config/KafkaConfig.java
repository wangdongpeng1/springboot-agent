package com.agent.springbootkafka.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * 配置类（创建主题）
 */
@Configuration
public class KafkaConfig {

    @Bean
    public NewTopic taskTopic() {
        return TopicBuilder.name("task-topic")
                .partitions(1)      // 必须为1，保证全局顺序
                .replicas(1)        // 单机环境用1，集群可调整
                .build();
    }
}
