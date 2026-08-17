package com.agent.springbootkafka.consumer;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 消费者
 */
@Service
@Slf4j
public class KafkaConsumerService {

    @KafkaListener(topics = "task-topic", groupId = "task-group")
    @Transactional(rollbackFor = Exception.class)
    public void consume(ConsumerRecord<String, String> record) {
        String task = record.value();
        log.info("📥 开始处理任务: {}，偏移量: {}", task, record.offset());

        try {
            // 模拟业务处理（耗时3秒）
            Thread.sleep(3000);
            log.info("✅ 完成处理任务: {}", task);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("❌ 处理任务被中断: {}，偏移量: {}", task, record.offset(), e);
            throw new RuntimeException("处理任务被中断: " + task, e);
        }
    }
}
