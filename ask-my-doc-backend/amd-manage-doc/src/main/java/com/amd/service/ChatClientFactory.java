package com.amd.service;

import com.amd.dto.AiRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatClientFactory {

    @Qualifier("ollamaChatClient")
    private final ChatClient ollamaChatClient;

    @Qualifier("openAiChatClient")
    private final ChatClient openAiChatClient;

    public ChatClient getChatClient(AiRequest request) {
        return switch (request.getAiProvider()) {
            case OLLAMA -> ollamaChatClient;
            case OPENAI -> openAiChatClient;
        };
    }
}
