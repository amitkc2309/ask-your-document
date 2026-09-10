package com.ayd.strategy;

import com.ayd.dto.AiRequest;
import com.ayd.enums.AiProvider;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.ChatOptions;

public interface AiProviderStrategy {
    ChatModel getChatModel()   ;
    AiProvider getProvider();
    ChatOptions.Builder getChatOptionsBuilder(AiRequest aiRequest);
    ChatClient getChatClient();
}
