package com.ayd.service;

import com.ayd.dto.ChatRequest;
import com.ayd.factory.AiProviderFactory;
import com.ayd.factory.RetrievalAugmentationAdvisorFactory;
import com.ayd.factory.SemanticCacheAdvisorFactory;
import com.ayd.strategy.AiProviderStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;
import org.springframework.ai.chat.cache.semantic.SemanticCacheAdvisor;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
@RequiredArgsConstructor
@Log
public class RagChatService {

    private final AiProviderFactory aiProviderFactory;
    private final RetrievalAugmentationAdvisorFactory ragAdvisorFactory;
    private final ChatMemory chatMemory;
    private final SemanticCacheAdvisorFactory semanticCacheAdvisorFactory;

    public Flux<String> ragSearch(ChatRequest request, String username, String userQuery) {
        long aiStartTime = System.currentTimeMillis();
        Filter.Expression vectorDBSearchFilter = new Filter.Expression(
                Filter.ExpressionType.EQ,
                new Filter.Key("uploadedBy"),
                new Filter.Value(username)
        );
        AiProviderStrategy aiProviderStrategy = aiProviderFactory.getStrategy(request.getAiRequest().getAiProvider());
        RetrievalAugmentationAdvisor ragAdvisor = ragAdvisorFactory
                .createRetrievalAdvisor(aiProviderStrategy.getChatModel(), request.getAiRequest().getModelName());

        SemanticCacheAdvisor semanticCacheAdvisor = semanticCacheAdvisorFactory.createSemanticCacheAdvisor(username);

        return aiProviderStrategy.getChatClient().prompt()
                .options(aiProviderStrategy.getChatOptionsBuilder(request.getAiRequest()))
                .advisors(advisorSpec ->
                        advisorSpec
                                .advisors(
                                        //Just Uncomment to use cache
                                        //semanticCacheAdvisor,
                                        MessageChatMemoryAdvisor.builder(chatMemory).build(),
                                        ragAdvisor)
                                .param(ChatMemory.CONVERSATION_ID, request.getConversationId())
                                .param(VectorStoreDocumentRetriever.FILTER_EXPRESSION, vectorDBSearchFilter))
                .user(userQuery)
                .stream()
                .content()
                .doOnComplete(() -> {
                    long aiEndTime = System.currentTimeMillis();
                    log.info("***AI total response time in ms:"+ (aiEndTime - aiStartTime));
                });
    }
}
