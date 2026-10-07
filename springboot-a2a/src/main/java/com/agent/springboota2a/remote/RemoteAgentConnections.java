package com.agent.springboota2a.remote;

import io.a2a.A2A;
import io.a2a.client.Client;
import io.a2a.client.TaskEvent;
import io.a2a.client.transport.jsonrpc.JSONRPCTransport;
import io.a2a.client.transport.jsonrpc.JSONRPCTransportConfig;
import io.a2a.spec.*;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class RemoteAgentConnections {

    @Value("${remote.agents.urls}")
    private List<String> agentUrls;

    private final Map<String, AgentCard> agentCards = new HashMap<>();

    @EventListener(ApplicationReadyEvent.class)
    public void discoverAgents() throws URISyntaxException {
        // The embedded A2A server is listening by the time this event is published.
        for (String url : agentUrls) {
            String path = new URI(url).getPath();
            AgentCard card = A2A.getAgentCard(url, path + "/card", null);
            this.agentCards.put(card.name(), card);
        }
    }

    @Tool(description = "Sends a task to a remote agent. Use this to delegate work to specialized agents.")
    public String sendMessage(
            @ToolParam(description = "The name of the agent, e.g. 'Weather Agent'") String agentName,
            @ToolParam(description = "The task description to send") String task)
            throws Exception {

        CompletableFuture<String> future = new CompletableFuture<>();

        Client client = Client.builder(agentCards.get(agentName))
                .withTransport(JSONRPCTransport.class, new JSONRPCTransportConfig())
                .addConsumers(List.of((event, card) -> {
                    if (event instanceof TaskEvent te && te.getTask().getArtifacts() != null)
                        future.complete(extractText(te.getTask().getArtifacts()));
                }))
                .build();

        client.sendMessage(
                new Message.Builder()
                        .role(Message.Role.USER)
                        .parts(List.of(new TextPart(task)))
                        .build());

        return future.get(60, TimeUnit.SECONDS);
    }

    public String getAgentDescriptions() {
        return agentCards.values().stream()
                .map(card -> card.name() + ": " + card.description())
                .collect(Collectors.joining("\n"));
    }

    private String extractText(List<Artifact> artifacts) {
        return artifacts.stream()
                .filter(Objects::nonNull)
                .flatMap(artifact -> artifact.parts().stream())
                .filter(TextPart.class::isInstance)
                .map(TextPart.class::cast)
                .map(TextPart::getText)
                .collect(Collectors.joining("\n"));
    }
}
