package com.ayd.dto;

import com.ayd.enums.DocumentStatus;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DocumentStatusEvent {
    private Long documentId;
    private String userId;
    private DocumentStatus documentStatus;
}
