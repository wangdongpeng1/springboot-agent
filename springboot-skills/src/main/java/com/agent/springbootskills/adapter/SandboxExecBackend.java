package com.agent.springbootskills.adapter;

import org.springaicommunity.agent.common.exec.ExecBackend;
import org.springaicommunity.agent.common.exec.ExecHandle;
import org.springaicommunity.agent.common.exec.ExecResult;
import org.springaicommunity.agent.common.exec.ExecSpec;
import org.springaicommunity.sandbox.Sandbox;

import java.time.Duration;

/**
 * 将 Spring AI Agent Utils 的 ExecBackend
 * 适配到 Agent Sandbox 的 Sandbox。
 *
 * Agent Utils:
 *   org.springaicommunity.agent.common.exec.ExecSpec
 *
 * Agent Sandbox:
 *   org.springaicommunity.sandbox.ExecSpec
 */
public class SandboxExecBackend implements ExecBackend {

    private final Sandbox sandbox;

    public SandboxExecBackend(Sandbox sandbox) {
        this.sandbox = sandbox;
    }

    /**
     * 同步执行 Shell 命令。
     */
    @Override
    public ExecResult run(ExecSpec spec) {

        // Agent Utils ExecSpec
        //      ↓
        // Sandbox ExecSpec

        org.springaicommunity.sandbox.ExecSpec sandboxSpec =
                org.springaicommunity.sandbox.ExecSpec.builder()
                        .shellCommand(spec.command())
                        .env(spec.env())
                        .timeout(Duration.ofMillis(spec.timeoutMillis()))
                        .build();

        try {

            org.springaicommunity.sandbox.ExecResult result =
                    sandbox.exec(sandboxSpec);

            // Sandbox ExecResult
            //      ↓
            // Agent Utils ExecResult

            return ExecResult.completed(
                    result.exitCode(),
                    result.stdout(),
                    result.stderr()
            );

        }
        catch (Exception e) {

            return ExecResult.launchFailed(
                    e.getMessage() != null
                            ? e.getMessage()
                            : e.getClass().getSimpleName()
            );
        }
    }

    /**
     * 当前 DockerSandbox 0.9.1 没有后台 Shell API，
     * 因此暂不支持 BashOutput / KillShell。
     */
    @Override
    public ExecHandle start(ExecSpec spec) {
        throw new UnsupportedOperationException(
                "Background shell is not supported by SandboxExecBackend"
        );
    }
}
