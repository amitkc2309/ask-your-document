package com.ayd.strategy;

import com.ayd.advisors.TokenUsageAuditAdvisor;
import com.ayd.dto.AiRequest;
import com.ayd.enums.AiProvider;
import lombok.extern.java.Log;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Log
public class OllamaProviderStrategy implements AiProviderStrategy {

    private final ChatClient chatClient;
    private final ChatModel chatModel;

    public OllamaProviderStrategy(OllamaChatModel chatModel,
                                  @Value("classpath:/templates/SystemPromptTemplate.st") Resource askAnswerSystemPrompt) {
        this.chatModel = chatModel;
        this.chatClient = ChatClient.builder(chatModel)
                .defaultSystem(askAnswerSystemPrompt)
                .defaultAdvisors(List.of(new SimpleLoggerAdvisor(), new TokenUsageAuditAdvisor()))
                .build();
    }

    @Override
    public ChatModel getChatModel() {
        return chatModel;
    }

    @Override
    public AiProvider getProvider() {
        return AiProvider.OLLAMA;
    }

    @Override
    public ChatOptions.Builder getChatOptionsBuilder(AiRequest aiRequest) {
        return OllamaChatOptions.builder()
                .model(aiRequest.getModelName())
                .temperature(0.7)
                .disableThinking();
    }

    @Override
    public ChatClient getChatClient() {
        return chatClient;
    }
}
