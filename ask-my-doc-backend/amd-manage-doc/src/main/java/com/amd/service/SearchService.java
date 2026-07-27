package com.amd.service;

import com.amd.dto.SearchRequest;
import com.amd.dto.QuestionResponse;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;


public interface SearchService {
    QuestionResponse search(SearchRequest question);

    Flux<String> chat(SearchRequest request, String userName);
}
