package com.ayd.service;

import com.ayd.dto.ChatRequest;
import com.ayd.dto.QuestionResponse;
import reactor.core.publisher.Flux;


public interface SearchService {
    QuestionResponse search(ChatRequest question);

    Flux<String> chat(ChatRequest request, String userName);
}
