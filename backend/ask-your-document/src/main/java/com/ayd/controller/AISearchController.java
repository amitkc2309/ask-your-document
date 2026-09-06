package com.ayd.controller;

import com.ayd.dto.ChatRequest;
import com.ayd.security.SecurityUtils;
import com.ayd.service.SearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/manage/search")
@RequiredArgsConstructor
public class AISearchController {

    private final SearchService searchService;

    @PostMapping(value = "/docs", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> stream(@RequestBody ChatRequest request) {
        String username = SecurityUtils.getUsername();
        return searchService.chat(request, username);
    }
}
