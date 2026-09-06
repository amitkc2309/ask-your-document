package com.ayd.controller;

import com.ayd.dto.ChatRequest;
import com.ayd.security.SecurityUtils;
import com.ayd.service.impl.AIChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
public class AiChatController {

    private final AIChatService chatService;

    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> stream(@RequestBody ChatRequest request) {
        String username = SecurityUtils.getUsername();
        String conservationId = chatService.createChatSessionForUser();
        request.setConversationId(conservationId);
        return chatService.chat(request, username);
    }

  /*  @GetMapping(value = "/session")
    public String getSession(@RequestBody ChatRequest request) {
        String username = SecurityUtils.getUsername();
        return chatService.chat(request, username);
    }*/
}
