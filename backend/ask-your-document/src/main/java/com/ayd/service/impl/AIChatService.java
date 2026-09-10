package com.ayd.service.impl;

import com.ayd.dto.ChatRequest;
import com.ayd.entity.ChatSessions;
import com.ayd.factory.AiProviderFactory;
import com.ayd.factory.RetrievalAugmentationAdvisorFactory;
import com.ayd.factory.SemanticCacheAdvisorFactory;
import com.ayd.repository.ChatSessionRepository;
import com.ayd.security.SecurityUtils;
import com.ayd.strategy.AiProviderStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.cache.semantic.SemanticCacheAdvisor;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AIChatService {

    private final AiProviderFactory aiProviderFactory;
    private final RetrievalAugmentationAdvisorFactory ragAdvisorFactory;
    private final ChatMemory chatMemory;
    private final ChatSessionRepository chatSessionRepository;
    private final SemanticCacheAdvisorFactory semanticCacheAdvisorFactory;

    public Flux<String> chat(ChatRequest request, String username) {
        if (username == null) {
            throw new IllegalArgumentException("username can not be null");
        }
        if (request.getConversationId() == null) {
            throw new IllegalArgumentException("ChatSessionId is required");
        }
        String userQuery = request.getKeyword();
        log.info("streamSearch username:{}", username);
        long aiStartTime = System.currentTimeMillis();
        Filter.Expression vectorDBSearchFilter = new Filter.Expression(
                        Filter.ExpressionType.EQ,
                        new Filter.Key("uploadedBy"),
                        new Filter.Value(username)
                );
        // Call to LLM
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
                    log.info("***AI total response time: {} ms", (aiEndTime - aiStartTime));
                });
    }

    public List<Message> getConversationById(String conversationId) {
        return chatMemory.get(conversationId);
    }

    @Transactional
    public String createChatSessionForUser() {
        ChatSessions chatSessions = new ChatSessions();
        chatSessions.setUsername(SecurityUtils.getUsername());
        UUID conservationId = UUID.randomUUID();
        chatSessions.setConversationId(conservationId.toString());
        chatSessions.setTitle(
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm"))
        );
        chatSessionRepository.save(chatSessions);
        return conservationId.toString();
    }

    public List<ChatSessions> getAllChatSessionsForUser() {
        return chatSessionRepository.findAllByUsername(SecurityUtils.getUsername());
    }

    @Transactional
    public void deleteConversationById(String conversationId) {
        chatMemory.clear(conversationId);
        chatSessionRepository.deleteByConversationId(conversationId);
    }
}
