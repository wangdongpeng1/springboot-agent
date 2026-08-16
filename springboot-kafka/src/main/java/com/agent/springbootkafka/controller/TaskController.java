package com.agent.springbootkafka.controller;

import com.agent.springbootkafka.producer.KafkaProducerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST接口
 */

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    @Autowired
    private KafkaProducerService producerService;

    /**
     * 批量提交任务（一次提交多个，但消费者会串行处理）
     * POST /api/tasks/batch
     * Body: ["task1", "task2", "task3", "task4", "task5"]
     */
    @PostMapping("/batch")
    public String batchSubmit(@RequestBody List<String> tasks) {
        producerService.batchSubmit(tasks);
        return "✅ 已提交 " + tasks.size() + " 个任务";
    }

    /**
     * 提交单个任务
     * POST /api/tasks
     * Body: "hello task"
     */
    @PostMapping
    public String submit(@RequestBody String task) {
        producerService.submit(task);
        return "✅ 已提交任务: " + task;
    }
}
