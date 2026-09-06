package com.ayd.config;

import com.ayd.advisors.TokenUsageAuditAdvisor;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepository;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.OllamaEmbeddingModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaEmbeddingOptions;
import org.springframework.ai.ollama.management.ModelManagementOptions;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.transformer.splitter.TextSplitter;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.util.List;

@Configuration
public class AiConfig {

    @Value("${spring.ai.ollama.embedding.options.model}")
    public String embeddingModelName;

    @Value("${application.chat-memory-max-messages}")
    public int chatMemoryMaxMessage;

    @Value("classpath:/templates/SystemPromptTemplate.st")
    Resource askAnswerSystemPrompt;

    @Bean
    public EmbeddingModel embeddingModel(OllamaApi ollamaApi, ObservationRegistry observationRegistry) {
        return new OllamaEmbeddingModel(ollamaApi, OllamaEmbeddingOptions.builder().model(embeddingModelName).build(),
                observationRegistry, ModelManagementOptions.defaults());
    }

    @Bean
    public TextSplitter tokenTextSplitter() {
        return TokenTextSplitter.builder()
                .withChunkSize(800)
                .withMinChunkSizeChars(350)
                .withMinChunkLengthToEmbed(10)
                .withMaxNumChunks(5000)
                .withPunctuationMarks(List.of('.', '?', '!', '\n'))
                .build();
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
    ChatMemory chatMemory(JdbcChatMemoryRepository  repository) {
        return MessageWindowChatMemory.builder().maxMessages(chatMemoryMaxMessage)
                .chatMemoryRepository(repository).build();
    }

    @Bean
    public ObservationRegistry observationRegistry() {
        return ObservationRegistry.create();
    }
}
