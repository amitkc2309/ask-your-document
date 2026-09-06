package com.ayd.service.impl;

import com.ayd.dto.DocumentProcessingMessage;
import com.ayd.dto.DocumentStatusEvent;
import com.ayd.entity.UserDocument;
import com.ayd.enums.DocumentStatus;
import com.ayd.enums.ProcessingStatus;
import com.ayd.repository.DocumentRepository;
import com.ayd.service.DocumentProcessorService;
import com.ayd.service.DocumentStatusPublisher;
import com.ayd.service.TextExtractionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
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
public class RagDocumentProcessorServiceImpl implements DocumentProcessorService {

    private final DocumentRepository documentRepository;
    private final TextExtractionService textExtractionService;
    private final TokenTextSplitter tokenTextSplitter;
    private final DocumentStatusPublisher documentStatusPublisher;
    private final VectorStore vectorStore;

    @KafkaListener(topics = "${application.kafka.document-upload-topic}", groupId = "${spring.kafka.consumer.group-id}")
    @Override
    public void processDocument(DocumentProcessingMessage message) {
        log.info("Processing document: {}", message.getDocumentId());
        // Get the document Metadata
        UserDocument document = documentRepository.findById(message.getDocumentId())
                .orElseThrow(() -> new RuntimeException("Document not found: " + message.getDocumentId()));
        if(document.getProcessingStatus() == ProcessingStatus.COMPLETED) return;
        try {
            documentStatusPublisher.publish(new DocumentStatusEvent(message.getDocumentId(), message.getUploadedBy(), DocumentStatus.PROCESSING));
            String rawText = textExtractionService.extractTextFromFile(document);
            //Convert to Spring AI Document
            Document sourceDocument = new Document(rawText);
            // This ensures every chunk knows it belongs to a 'uploadedBy' and Document ID '123'
            sourceDocument.getMetadata().put("documentId", document.getId());
            sourceDocument.getMetadata().put("author", document.getAuthor());
            sourceDocument.getMetadata().put("uploadedBy", document.getUploadedBy());
            sourceDocument.getMetadata().put("type", document.getDocumentType().name());

            //Split into chunks
            List<Document> chunks = tokenTextSplitter.apply(List.of(sourceDocument));
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
                enrichedChunks.add(new Document(newText, metadata));
            }
            documentStatusPublisher.publish(new DocumentStatusEvent(message.getDocumentId(), message.getUploadedBy(), DocumentStatus.EMBEDDING));
            /*load Data Into VectorStore.
            VectorStore will also embed the chunks*/
            vectorStore.add(enrichedChunks);
            documentStatusPublisher.publish(new DocumentStatusEvent(message.getDocumentId(), message.getUploadedBy(), DocumentStatus.INDEXED));
            // Update Database Status
            document.setProcessingStatus(ProcessingStatus.COMPLETED);
            document.setProcessedDate(LocalDateTime.now());
            documentRepository.save(document);
            documentStatusPublisher.publish(new DocumentStatusEvent(message.getDocumentId(), message.getUploadedBy(), DocumentStatus.READY));
        } catch (IOException e) {
            log.error("Failed to process document: {} with error {}", document.getId(), e);
            document.setProcessingStatus(ProcessingStatus.FAILED);
            documentRepository.save(document);
        }
    }
}
