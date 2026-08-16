package com.agent.springbootkafka.consumer;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

/**
 * 消费者
 */
@Service
public class KafkaConsumerService {

    @KafkaListener(topics = "task-topic", groupId = "task-group")
    public void consume(ConsumerRecord<String, String> record, Acknowledgment ack) {
        String task = record.value();
        System.out.println("📥 开始处理任务: " + task + "，偏移量: " + record.offset());

        try {
            // 模拟业务处理（耗时3秒）
            Thread.sleep(3000);
            System.out.println("✅ 完成处理任务: " + task);

            // 手动提交Offset（只有成功处理才提交）
            ack.acknowledge();
            System.out.println("📍 已提交Offset: " + record.offset());

        } catch (Exception e) {
            System.err.println("❌ 处理任务失败: " + task + "，错误: " + e.getMessage());
            // 注意：不调用 ack.acknowledge()，消息会重试
        }
    }
}
