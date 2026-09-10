package com.ayd.factory;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.preretrieval.query.transformation.CompressionQueryTransformer;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RetrievalAugmentationAdvisorFactory {

    private final VectorStore vectorStore;

    @Value("${application.vector-store.top-k}")
    Integer topK;
    @Value("${application.vector-store.similarity-threshold}")
    Double similarityThreshold;


    public RetrievalAugmentationAdvisor createRetrievalAdvisor(
            ChatModel chatModel,
            String requestedModelName) {
        ChatOptions options = ChatOptions.builder()
                .model(requestedModelName)
                .temperature(0.0)
                .build();
        ChatClient.Builder compressionClientBuilder = ChatClient.builder(chatModel)
                .defaultOptions(options);
        CompressionQueryTransformer queryTransformer = CompressionQueryTransformer.builder()
                .chatClientBuilder(compressionClientBuilder)
                .build();
        return RetrievalAugmentationAdvisor.builder()
                .queryTransformers(queryTransformer)
                .documentRetriever(VectorStoreDocumentRetriever.builder()
                        .vectorStore(vectorStore)
                        .topK(topK)
                        .similarityThreshold(similarityThreshold)
                        .build())
                .build();
    }
}
