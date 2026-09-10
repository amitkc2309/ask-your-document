package com.ayd.config;

import io.micrometer.observation.ObservationRegistry;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepository;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.ollama.OllamaEmbeddingModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaEmbeddingOptions;
import org.springframework.ai.ollama.management.ModelManagementOptions;
import org.springframework.ai.transformer.splitter.TextSplitter;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class AiConfig {

    @Value("${spring.ai.ollama.embedding.model}")
    public String embeddingModelName;

    @Value("${application.chat-memory-max-messages}")
    public int chatMemoryMaxMessage;


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
