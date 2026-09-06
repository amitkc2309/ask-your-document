package com.ayd.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRequest {
    
    @NotBlank(message = "Question is required")
    private String keyword;
    
    private Integer maxResults = 5;
    
    private Integer snippetLength = 200;

    private AiRequest aiRequest;

    private String conversationId;
}