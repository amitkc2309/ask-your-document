package com.ayd.strategy;

import com.ayd.advisors.TokenUsageAuditAdvisor;
import com.ayd.dto.AiRequest;
import com.ayd.enums.AiProvider;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OpenAiProviderStrategy implements AiProviderStrategy {

    private final ChatClient chatClient;
    private final ChatModel chatModel;

    public OpenAiProviderStrategy(OpenAiChatModel chatModel) {
        this.chatModel = chatModel;
        this.chatClient = ChatClient.builder(chatModel)
                .defaultAdvisors(List.of(new SimpleLoggerAdvisor(), new TokenUsageAuditAdvisor()))
                .build();
    }

    @Override
    public ChatModel getChatModel() {
        return this.chatModel;
    }

    @Override
    public AiProvider getProvider() {
        return AiProvider.OPENAI;
    }

    @Override
    public ChatOptions getChatOptions(AiRequest aiRequest) {
        return OpenAiChatOptions.builder()
                .model(aiRequest.getModelName())
                .build();
    }

    @Override
    public ChatClient getChatClient() {
        return chatClient;
    }
}
