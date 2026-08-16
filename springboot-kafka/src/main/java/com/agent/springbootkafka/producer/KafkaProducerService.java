package com.agent.springbootkafka.producer;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 生产者
 */
@Service
public class KafkaProducerService {

    private final KafkaTemplate<String, String> kafkaTemplate;

    private static final String TOPIC_1 = "task-topic";

    public KafkaProducerService(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * 批量提交任务到Kafka（使用相同的key保证进入同一分区）
     */
    public void batchSubmit(List<String> tasks) {
        for (String task : tasks) {
            // 固定key="batch"，保证所有消息进入同一个分区
            kafkaTemplate.send(TOPIC_1, "batch", task);
            System.out.println("✅ 提交任务: " + task);
        }
    }

    /**
     * 提交单个任务
     */
    public void submit(String task) {
        kafkaTemplate.send(TOPIC_1, "batch", task);
        System.out.println("✅ 提交任务: " + task);
    }
}
