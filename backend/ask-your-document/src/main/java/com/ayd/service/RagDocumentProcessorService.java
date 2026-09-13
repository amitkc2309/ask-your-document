package com.ayd.service;

import com.ayd.dto.DocumentProcessingMessage;
import com.ayd.dto.DocumentStatusEvent;
import com.ayd.entity.UserDocument;
import com.ayd.enums.DocumentStatus;
import com.ayd.enums.ProcessingStatus;
import com.ayd.pubsub.DocumentStatusPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TextSplitter;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class RagDocumentProcessorService {

    private final UserDocumentsService userDocumentsService;
    private final TextExtractionService textExtractionService;
    private final TextSplitter textSplitter;
    private final DocumentStatusPublisher documentStatusPublisher;
    private final VectorStoreService vectorStoreService;

    @KafkaListener(topics = "${application.kafka.document-upload-topic}", groupId = "${spring.kafka.consumer.group-id}")
    public void processDocument(DocumentProcessingMessage message) {
        log.info("Processing document: {}", message.getDocumentId());
        // Get the document Metadata
        UserDocument document = userDocumentsService.getDocumentById(message.getDocumentId(), message.getUploadedBy());
        if(document.getProcessingStatus() == ProcessingStatus.COMPLETED) return;
        try {
            documentStatusPublisher.publish(new DocumentStatusEvent(message.getDocumentId(), message.getUploadedBy(), DocumentStatus.PROCESSING));
            List<Document> documents = textExtractionService.extractTextFromFile(document);
            //Split into chunks
            List<Document> chunks = textSplitter.split(documents);
            // Enrich chunks
            List<Document> enrichedChunks = new ArrayList<>();
            for (Document chunk : chunks) {
                String newText = "Title: " + document.getTitle() + "\n\n" + chunk.getText();
                Map<String, Object> metadata = new HashMap<>(chunk.getMetadata());
                metadata.put("documentId", document.getId());
                metadata.put("title", document.getTitle());
                metadata.put("author", document.getAuthor());
                metadata.put("uploadedBy", document.getUploadedBy());
                metadata.put("type", document.getDocumentType().name());
                metadata.put("source", document.getFileName());
                enrichedChunks.add(new Document(newText, metadata));
            }
            documentStatusPublisher.publish(new DocumentStatusEvent(message.getDocumentId(), message.getUploadedBy(), DocumentStatus.EMBEDDING));
            vectorStoreService.storeDocuments(enrichedChunks);
            documentStatusPublisher.publish(new DocumentStatusEvent(message.getDocumentId(), message.getUploadedBy(), DocumentStatus.INDEXED));
            // Update Database Status
            document.setProcessingStatus(ProcessingStatus.COMPLETED);
            document.setProcessedDate(LocalDateTime.now());
            userDocumentsService.saveUserDocument(document);
            documentStatusPublisher.publish(new DocumentStatusEvent(message.getDocumentId(), message.getUploadedBy(), DocumentStatus.READY));
        } catch (IOException e) {
            log.error("Failed to process document: {} with error {}", document.getId(), e);
            document.setProcessingStatus(ProcessingStatus.FAILED);
            userDocumentsService.saveUserDocument(document);
        }
    }
}
