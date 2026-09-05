package com.ayd.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentProcessingMessage {
    private Long documentId;
    private String fileName;
    private String contentType;
    private String filePath;
    private String title;
    private String author;
    private String uploadedBy;
    private LocalDateTime uploadDate;
}