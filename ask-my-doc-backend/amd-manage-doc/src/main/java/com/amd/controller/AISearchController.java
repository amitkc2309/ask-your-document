package com.amd.controller;

import com.amd.dto.SearchRequest;
import com.amd.security.SecurityUtils;
import com.amd.service.SearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/manage/search")
@ConditionalOnProperty(name = "application.ai-mode", havingValue = "true")
@RequiredArgsConstructor
public class AISearchController {

    private final SearchService searchService;

    @PostMapping(value = "/docs", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> stream(@RequestBody SearchRequest request) {
        String username = SecurityUtils.getUsername();
        return searchService.chat(request, username);
    }
}
