package com.amd.service;

import com.amd.dto.ChatRequest;
import com.amd.dto.QuestionResponse;
import reactor.core.publisher.Flux;


public interface SearchService {
    QuestionResponse search(ChatRequest question);

    Flux<String> chat(ChatRequest request, String userName);
}
