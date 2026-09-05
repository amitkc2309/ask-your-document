package com.ayd.controller;

import com.ayd.dto.ChatRequest;
import com.ayd.dto.QuestionResponse;
import com.ayd.service.SearchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/manage/search")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "application.ai-mode", havingValue = "false")
public class SearchController {

    private final SearchService searchService;

    @PostMapping("/docs")
    public ResponseEntity<QuestionResponse> search(
            @Valid @RequestBody ChatRequest question,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        return ResponseEntity.ok(searchService.search(question));
    }
}
