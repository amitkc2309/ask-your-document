package com.ayd.service.impl;

import com.ayd.dto.DocumentProcessingMessage;
import com.ayd.entity.Document;
import com.ayd.enums.ProcessingStatus;
import com.ayd.entity.elasticsearch.DocumentIndex;
import com.ayd.repository.DocumentRepository;
import com.ayd.repository.elasticsearch.DocumentIndexRepository;
import com.ayd.service.DocumentProcessorService;
import com.ayd.service.TextExtractionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "application.ai-mode", havingValue = "false")
public class ElasticSearchDocumentProcessorServiceImpl implements DocumentProcessorService {

    private final DocumentRepository documentRepository;
    private final DocumentIndexRepository documentIndexRepository;
    private final TextExtractionService textExtractionService;

    /**
     * Kafka listener for document processing messages.
     *
     * @param message The document processing message
     */
    @KafkaListener(topics = "${application.kafka.document-upload-topic}", groupId = "${spring.kafka.consumer.group-id}")
    @Override
    public void processDocument(DocumentProcessingMessage message) {
        log.info("Processing document: {}", message.getDocumentId());

        try {
            // Get the document from the database
            Document document = documentRepository.findById(message.getDocumentId())
                    .orElseThrow(() -> new RuntimeException("Document not found: " + message.getDocumentId()));

            // Update document status to PROCESSING
            document.setProcessingStatus(ProcessingStatus.PROCESSING);
            documentRepository.save(document);

            // Extract text from the document
            String textContent = textExtractionService.extractTextFromFile(document);
            log.info("*******************Doc content: {}", textContent);
            // Update document with extracted text and status
            document.setProcessingStatus(ProcessingStatus.COMPLETED);
            document.setProcessedDate(LocalDateTime.now());
            documentRepository.save(document);

            // Index the document in Elasticsearch
            DocumentIndex documentIndex = DocumentIndex.builder()
                    .id(document.getId().toString())
                    .title(document.getTitle())
                    .author(document.getAuthor())
                    .content(textContent)
                    .fileName(document.getFileName())
                    .documentType(document.getDocumentType())
                    .uploadedBy(document.getUploadedBy())
                    .databaseId(document.getId())
                    .build();

            documentIndexRepository.save(documentIndex);

            // Update document with Elasticsearch ID
            document.setElasticsearchId(documentIndex.getId());
            documentRepository.save(document);

            log.info("Document processed successfully: {}", document.getId());
        } catch (Exception e) {
            log.error("Error processing document: {}", message.getDocumentId(), e);

            try {
                // Update document status to FAILED
                Document document = documentRepository.findById(message.getDocumentId()).orElse(null);
                if (document != null) {
                    document.setProcessingStatus(ProcessingStatus.FAILED);
                    documentRepository.save(document);
                }
            } catch (Exception ex) {
                log.error("Error updating document status: {}", message.getDocumentId(), ex);
            }
        }
    }
}
