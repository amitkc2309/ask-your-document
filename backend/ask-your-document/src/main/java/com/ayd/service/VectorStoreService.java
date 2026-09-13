package com.ayd.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class VectorStoreService {

    private final VectorStore vectorStore;

    @Value("${application.vector-store.top-k}")
    Integer topK;
    @Value("${application.vector-store.similarity-threshold}")
    Double similarityThreshold;

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

    public List<Document> search(String userId, String query) {
        if (userId == null || userId.isBlank()) {
            throw new AccessDeniedException("Missing authenticated user");
        }
        Filter.Expression vectorDBSearchFilter = new Filter.Expression(
                Filter.ExpressionType.EQ,
                new Filter.Key("uploadedBy"),
                new Filter.Value(userId)
        );
        SearchRequest sr = SearchRequest.builder()
                .query(query)
                .topK(topK)
                .similarityThreshold(similarityThreshold)
                .filterExpression(vectorDBSearchFilter)
                .build();
        return vectorStore.similaritySearch(sr);
    }

    public void storeDocuments(List<Document> documents) {
        /*load Data Into VectorStore.
            VectorStore will also embed the documents*/
        vectorStore.add(documents);
    }
}
