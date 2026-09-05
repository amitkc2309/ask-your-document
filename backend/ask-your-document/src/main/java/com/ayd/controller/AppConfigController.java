package com.ayd.controller;

import com.ayd.enums.AiProvider;
import com.ayd.service.AiModelsService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/config")
@RequiredArgsConstructor
public class AppConfigController {

    @Value("${application.ai-mode}")
    private boolean aiEnabled;

    private final AiModelsService aiModelsService;

    @GetMapping("/ai-mode")
    public Map<String, Object> getConfig() {
        return Map.of(
                "aiMode", aiEnabled
        );
    }
    @GetMapping("/ai-models")
    public Map<AiProvider, List<String>> getAiModels() {
        return aiModelsService.getAiModels();
    }

}
