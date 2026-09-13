package com.ayd.strategy;

import com.ayd.advisors.TokenUsageAuditAdvisor;
import com.ayd.dto.AiRequest;
import com.ayd.enums.AiProvider;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.model.chat.client.autoconfigure.ChatClientBuilderConfigurer;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OpenAiProviderStrategy implements AiProviderStrategy {

    private final ChatClient chatClient;
    private final ChatModel chatModel;

    public OpenAiProviderStrategy(OpenAiChatModel chatModel,
                                  ChatClientBuilderConfigurer chatClientBuilderConfigurer,
                                  @Value("classpath:/templates/SystemPromptTemplate.st") Resource askAnswerSystemPrompt) {
        this.chatModel = chatModel;
        ChatClient.Builder builder = ChatClient.builder(chatModel);
        chatClientBuilderConfigurer.configure(builder);
        this.chatClient = builder
                .defaultSystem(askAnswerSystemPrompt)
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
    public ChatOptions.Builder getChatOptionsBuilder(AiRequest aiRequest) {
        return OpenAiChatOptions.builder()
                .model(aiRequest.getModelName());
    }

    @Override
    public ChatClient getChatClient() {
        return chatClient;
    }
}
