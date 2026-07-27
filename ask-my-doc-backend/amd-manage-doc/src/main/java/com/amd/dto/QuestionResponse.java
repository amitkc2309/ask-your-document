package com.amd.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionResponse {
    
    private String question;

    private String answer;
    
    private List<DocumentSnippet> snippets;
    
    private int totalResults;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DocumentSnippet {
        private Long documentId;
        private String documentTitle;
        private String author;
        private String snippet;
        private double relevanceScore;
    }
}