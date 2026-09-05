package com.amd.service;

import com.amd.dto.ChatRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "application.ai-mode", havingValue = "true")
public class QueryTransformationService {

    private final ChatClientFactory chatClientFactory;

    @Value("classpath:/templates/QueryRewritePromptTemplate.st")
    Resource rewritePrompt;

    public String transform(ChatRequest request) {
        log.info("Original Query: {}", request.getKeyword());
        ChatClient chatClient = chatClientFactory.getChatClient(request.getAiRequest());
        String transformedQuery = chatClient.prompt()
                .user(u -> u.text(rewritePrompt).param("query", request.getKeyword()))
                .call()
                .content();
        log.info("Transformed Query: {}", transformedQuery);
        return transformedQuery;
    }
}
