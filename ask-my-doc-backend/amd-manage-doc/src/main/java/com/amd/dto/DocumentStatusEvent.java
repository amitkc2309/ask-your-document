package com.amd.dto;

import com.amd.enums.DocumentStatus;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DocumentStatusEvent {
    private Long documentId;
    private String username;
    private DocumentStatus documentStatus;
}
