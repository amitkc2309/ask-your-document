package com.ayd.config;

import com.ayd.advisors.TokenUsageAuditAdvisor;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.OllamaEmbeddingModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaEmbeddingOptions;
import org.springframework.ai.ollama.management.ModelManagementOptions;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.util.List;

@Configuration
@ConditionalOnProperty(name = "application.ai-mode", havingValue = "true")
public class AiConfig {

    @Value("${spring.ai.ollama.embedding.options.model}")
    public String embeddingModelName;

    @Value("classpath:/templates/SystemPromptTemplate.st")
    Resource askAnswerSystemPrompt;

    @Bean
    public EmbeddingModel embeddingModel(OllamaApi ollamaApi, ObservationRegistry observationRegistry) {
        return new OllamaEmbeddingModel(ollamaApi, OllamaEmbeddingOptions.builder().model(embeddingModelName).build(),
                observationRegistry, ModelManagementOptions.defaults());
    }

    @Bean
    public TokenTextSplitter tokenTextSplitter() {
        return new TokenTextSplitter(
                300,
                100,
                5,
                10000,
                true,
                List.of('.', ',', '?', '!', '\n')
        );
    }

    @Bean("ollamaChatClient")
    ChatClient ollamaChatClient(
            OllamaChatModel model) {
        return ChatClient.builder(model)
                .defaultSystem(askAnswerSystemPrompt)
                .defaultAdvisors(List.of(new SimpleLoggerAdvisor(),new TokenUsageAuditAdvisor()))
                .build();
    }

    @Bean("openAiChatClient")
    ChatClient openAiChatClient(
            OpenAiChatModel model) {
        return ChatClient.builder(model)
                .defaultSystem(askAnswerSystemPrompt)
                .defaultAdvisors(new SimpleLoggerAdvisor())
                .build();
    }

    @Bean
    public ObservationRegistry observationRegistry() {
        return ObservationRegistry.create();
    }
}
