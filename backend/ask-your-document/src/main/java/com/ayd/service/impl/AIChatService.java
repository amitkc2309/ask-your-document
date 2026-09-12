package com.ayd.service.impl;

import com.ayd.dto.ChatRequest;
import com.ayd.entity.ChatSessions;
import com.ayd.repository.ChatSessionRepository;
import com.ayd.security.SecurityUtils;
import com.ayd.service.RagChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Log
public class AIChatService {

    private final ChatMemory chatMemory;
    private final ChatSessionRepository chatSessionRepository;
    private final RagChatService ragChatService;

    public Flux<String> chat(ChatRequest request, String username) {
        if (username == null) {
            throw new IllegalArgumentException("username can not be null");
        }
        if (request.getConversationId() == null) {
            throw new IllegalArgumentException("ChatSessionId is required");
        }
        String userQuery = request.getKeyword();
        log.info("streamSearch username:"+ username);
        return ragChatService.ragSearch(request, username, userQuery);
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
