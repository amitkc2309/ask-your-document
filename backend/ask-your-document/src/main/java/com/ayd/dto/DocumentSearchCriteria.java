package com.ayd.dto;

import com.ayd.enums.DocumentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentSearchCriteria {
    private String title;
    private String author;
    private DocumentType documentType;
    private String keyword;
}