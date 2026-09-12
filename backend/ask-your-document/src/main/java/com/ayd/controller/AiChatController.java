package com.ayd.controller;

import com.ayd.dto.ChatRequest;
import com.ayd.dto.ChatSessionsDto;
import com.ayd.security.SecurityUtils;
import com.ayd.service.impl.AIChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;
import org.springframework.ai.chat.messages.Message;
import org.springframework.beans.BeanUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
@Log
public class AiChatController {

    private final AIChatService chatService;

    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> stream(@RequestBody ChatRequest request) {
        String userId = SecurityUtils.getUserId();
        return chatService.chat(request, userId);
    }

    @PostMapping(value = "/new-chat")
    public ResponseEntity<String> createNewChatSessionForUser() {
        String conservationId = chatService.createChatSessionForUser();
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(conservationId);
    }

    @GetMapping(value = "/all-conversations")
    public List<ChatSessionsDto> getAllChatSessionsForUser() {
        return chatService.getAllChatSessionsForUser()
                .stream()
                .map(session -> {
                    ChatSessionsDto dto = new ChatSessionsDto();
                    BeanUtils.copyProperties(session, dto);
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @GetMapping(value = "/conversation/{conversationId}")
    public List<Message> getConversationById(@PathVariable String conversationId) {
        return chatService.getConversationById(conversationId);
    }

    @DeleteMapping("/conversation/{conversationId}")
    public ResponseEntity<Void> deleteDocument(@PathVariable String conversationId) {
        chatService.deleteConversationById(conversationId);
        return ResponseEntity.ok().build();
    }
}
