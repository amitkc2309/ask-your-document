package com.ayd.service.impl;

import com.ayd.service.DBServices;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class VectorDBServices implements DBServices {

    private final VectorStore vectorStore;

    @Override
    public void deleteByDocumentId(Long documentId) {
        try {
            vectorStore.delete(
                    "documentId == '" + documentId + "'"
            );
            log.info("Deleted all chunks for documentId={}", documentId);

        } catch (Exception e) {
            log.error("Failed to delete documentId={}", documentId, e);
            throw new RuntimeException(e);
        }
    }
}
