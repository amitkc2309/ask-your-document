package com.amd.dto;

import org.springframework.ai.document.Document;

public record RerankedDocument(Document document, double score) {}
