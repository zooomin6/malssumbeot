package com.malssumbeot.orchestrator;

import com.anthropic.client.AnthropicClient;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import org.springframework.stereotype.Component;

@Component
public class AnthropicClaudeChat implements ClaudeChat {

    private final AnthropicClient client;

    public AnthropicClaudeChat(AnthropicClient client) {
        this.client = client;
    }

    @Override
    public String complete(String model, int maxTokens, String systemPrompt, String userMessage) {
        MessageCreateParams params = MessageCreateParams.builder()
                .model(model)
                .maxTokens((long) maxTokens)
                .system(systemPrompt)
                .addUserMessage(userMessage)
                .build();

        Message response = client.messages().create(params);
        StringBuilder sb = new StringBuilder();
        response.content().stream()
                .flatMap(block -> block.text().stream())
                .forEach(text -> sb.append(text.text()));
        return sb.toString();
    }
}
