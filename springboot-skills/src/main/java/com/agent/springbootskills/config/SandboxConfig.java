package com.agent.springbootskills.config;
import com.agent.springbootskills.adapter.SandboxExecBackend;
import org.springaicommunity.sandbox.Sandbox;
import org.springaicommunity.sandbox.docker.DockerSandbox;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SandboxConfig {

    @Value("${agent.sandbox.image:python:3.12-slim}")
    private String sandboxImage;

    /**
     * Docker Sandbox。
     *
     * 底层使用 Testcontainers GenericContainer。
     */
    @Bean(destroyMethod = "close")
    public Sandbox dockerSandbox() {

        return DockerSandbox.builder()
                .image(sandboxImage)
                .build();
    }

    /**
     * 将 Agent Sandbox 适配成 Spring AI Agent Utils
     * 所需要的 ExecBackend。
     */
    @Bean
    public SandboxExecBackend sandboxExecBackend(
            Sandbox dockerSandbox) {

        return new SandboxExecBackend(dockerSandbox);
    }
}
