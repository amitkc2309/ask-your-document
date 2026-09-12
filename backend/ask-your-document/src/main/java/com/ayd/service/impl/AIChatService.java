package com.ayd.service.impl;

import com.ayd.dto.ChatRequest;
import com.ayd.entity.ChatSessions;
import com.ayd.factory.AiProviderFactory;
import com.ayd.repository.ChatSessionRepository;
import com.ayd.security.SecurityUtils;
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

    public Flux<String> chat(ChatRequest request, String username) {
        if (username == null) {
            throw new IllegalArgumentException("username can not be null");
        }
        if (request.getConversationId() == null) {
            throw new IllegalArgumentException("ChatSessionId is required");
        }
        //TODO Security: validate ConversationId belongs to username
        String userQuery = request.getKeyword();
        log.info("streamSearch username:"+ username);
        long aiStartTime = System.currentTimeMillis();
        AiProviderStrategy aiProviderStrategy = aiProviderFactory.getStrategy(request.getAiRequest().getAiProvider());
        //SemanticCacheAdvisor semanticCacheAdvisor = semanticCacheAdvisorFactory.createSemanticCacheAdvisor(username);

        return aiProviderStrategy.getChatClient().prompt()
                .options(aiProviderStrategy.getChatOptionsBuilder(request.getAiRequest()))
                .tools(vectorDocumentSearchTool)
                .toolContext(Map.of("username", username))
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
