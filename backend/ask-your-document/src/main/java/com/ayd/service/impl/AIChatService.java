package com.ayd.service.impl;

import com.ayd.dto.ChatRequest;
import com.ayd.entity.ChatSessions;
import com.ayd.factory.AiProviderFactory;
import com.ayd.repository.ChatSessionRepository;
import com.ayd.security.SecurityUtils;
import com.ayd.service.ConversationAccessService;
import com.ayd.strategy.AiProviderStrategy;
import com.ayd.tools.VectorStoreDocumentSearchTool;
import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Log
public class AIChatService {

    private final ChatMemory chatMemory;
    private final ChatSessionRepository chatSessionRepository;
    //private final SemanticCacheAdvisorFactory semanticCacheAdvisorFactory;
    private final AiProviderFactory aiProviderFactory;
    private final VectorStoreDocumentSearchTool vectorDocumentSearchTool;
    private final ConversationAccessService conversationAccessService;

    public Flux<String> chat(ChatRequest request, String userId) {
        if (userId == null) {
            throw new IllegalArgumentException("userId can not be null");
        }
        if (request.getConversationId() == null) {
            throw new IllegalArgumentException("ChatSessionId is required");
        }
        conversationAccessService.validateConversationAccess(request.getConversationId(), userId);
        String userQuery = request.getUserMessage();
        log.info("streamSearch userId:"+ userId);
        long aiStartTime = System.currentTimeMillis();
        AiProviderStrategy aiProviderStrategy = aiProviderFactory.getStrategy(request.getAiRequest().getAiProvider());
        //SemanticCacheAdvisor semanticCacheAdvisor = semanticCacheAdvisorFactory.createSemanticCacheAdvisor(userId);

        return aiProviderStrategy.getChatClient().prompt()
                .options(aiProviderStrategy.getChatOptionsBuilder(request.getAiRequest()))
                .tools(vectorDocumentSearchTool)
                .toolContext(Map.of("userId", userId))
                .advisors(advisorSpec ->
                        advisorSpec
                                .advisors(
                                        //Just Uncomment to use cache
                                        //semanticCacheAdvisor,
                                        MessageChatMemoryAdvisor.builder(chatMemory).build())
                                .param(ChatMemory.CONVERSATION_ID, request.getConversationId()))

                .user(userQuery)
                .stream()
                .content()
                .doOnComplete(() -> {
                    long aiEndTime = System.currentTimeMillis();
                    log.info("***AI total response time in ms:"+ (aiEndTime - aiStartTime));
                })
                .onErrorResume(ex -> Flux.just("Sorry, something went wrong. Please try again."));
    }

    public List<Message> getConversationById(String conversationId) {
        return chatMemory.get(conversationId);
    }

    @Transactional
    public String createChatSessionForUser() {
        ChatSessions chatSessions = new ChatSessions();
        chatSessions.setUserId(SecurityUtils.getUserId());
        UUID conservationId = UUID.randomUUID();
        chatSessions.setConversationId(conservationId.toString());
        chatSessions.setTitle(
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm"))
        );
        chatSessionRepository.save(chatSessions);
        return conservationId.toString();
    }

    public List<ChatSessions> getAllChatSessionsForUser() {
        return chatSessionRepository.findAllByUserId(SecurityUtils.getUserId());
    }

    @Transactional
    public void deleteConversationById(String conversationId) {
        conversationAccessService.validateConversationAccess(conversationId,SecurityUtils.getUserId());
        chatMemory.clear(conversationId);
        chatSessionRepository.deleteByConversationId(conversationId);
        conversationAccessService.invalidateConversationAccess(conversationId,SecurityUtils.getUserId());
    }

}
