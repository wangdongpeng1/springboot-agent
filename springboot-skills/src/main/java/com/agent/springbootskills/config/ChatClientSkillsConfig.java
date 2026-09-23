package com.agent.springbootskills.config;

import com.agent.springbootskills.adapter.SandboxExecBackend;
import lombok.extern.slf4j.Slf4j;
import org.springaicommunity.agent.tools.FileSystemTools;
import org.springaicommunity.agent.tools.ShellTools;
import org.springaicommunity.agent.tools.SkillsTool;
import org.springaicommunity.sandbox.Sandbox;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.deepseek.DeepSeekChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.stream.Stream;

@Configuration
@Slf4j
public class ChatClientSkillsConfig {

    @Value("${agent.workspace:./workspace}")
    private String workspaceDirectory;

    @Bean
    public ChatClient chatClientSkills(DeepSeekChatModel chatModel,
                                       SandboxExecBackend execBackend,
                                       Sandbox dockerSandbox) throws IOException, URISyntaxException {
        // 宿主机工作区
        Path workspaceRoot = Paths.get(workspaceDirectory)
                .toAbsolutePath()
                .normalize();
        // 宿主机 skills 目录
        Path skillsRoot = workspaceRoot.resolve("skills");
        // 同时复制到：
        // 1. 宿主机 workspace/skills
        // 2. DockerSandbox /work/skills
        copySkillsToWorkspace(
                new ClassPathResource("skills"),
                skillsRoot,
                dockerSandbox
        );

        return ChatClient.builder(chatModel)
                .defaultSystem("""
                        你是一个乐于助人的助手。
                        请根据用户请求选择合适的 Skill。
                        如果 Skill 要求调用工具，必须实际调用工具完成任务，
                        不要直接根据自己的能力计算。
                        """)
                .defaultTools(
                        // Skill 文件从宿主机读取
                        SkillsTool.builder()
                                .addSkillsDirectory(skillsRoot.toString())
                                .build(),
                        // Shell 命令进入 DockerSandbox 执行
                        ShellTools.builder()
                                .execBackend(execBackend)
                                .build(),
                        // 文件系统操作
                        FileSystemTools.builder()
                                .build()
                )
                .build();
    }

    /**
     * 将 classpath:/skills 同步到：
     *
     * 宿主机：
     * workspace/skills
     *
     * DockerSandbox：
     * /work/skills
     */
    private void copySkillsToWorkspace(Resource skillsResource,
                                       Path targetRoot,
                                       Sandbox dockerSandbox) throws IOException, URISyntaxException {

        Path sourceRoot = Paths.get(skillsResource.getURI());

        // =========================================================
        // 1. 复制到宿主机 workspace/skills
        // =========================================================
        Files.createDirectories(targetRoot);

        try (Stream<Path> stream = Files.walk(sourceRoot)) {
            stream.forEach(source -> {
                try {
                    Path relative = sourceRoot.relativize(source);
                    Path target = targetRoot.resolve(relative);

                    if (Files.isDirectory(source)) {
                        Files.createDirectories(target);
                    } else {
                        Files.createDirectories(target.getParent());
                        Files.copy(
                                source,
                                target,
                                StandardCopyOption.REPLACE_EXISTING
                        );
                    }
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        }

        // =========================================================
        // 2. 同步到 DockerSandbox /work/skills
        // =========================================================
        try (Stream<Path> stream = Files.walk(targetRoot)) {
            stream
                    .filter(Files::isRegularFile)
                    .forEach(file -> {
                        try {
                            log.info("读取 Skill 文件: {}", file);

                            Path relative = targetRoot.relativize(file);
                            // DockerSandbox 的工作目录是 /work
                            String sandboxPath = "skills/"
                                    + relative.toString()
                                    .replace('\\', '/');
                            String content = Files.readString(
                                    file,
                                    StandardCharsets.UTF_8
                            );
                            dockerSandbox.files().create(
                                    sandboxPath,
                                    content
                            );
                        } catch (IOException e) {
                            throw new UncheckedIOException("读取文件失败: " + file, e);
                        }
                    });
        }
    }
}
