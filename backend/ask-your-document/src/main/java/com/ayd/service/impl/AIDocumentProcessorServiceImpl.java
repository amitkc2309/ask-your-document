package com.ayd.service.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import com.ayd.config.ElasticSearchConfig;
import com.ayd.dto.DocumentProcessingMessage;
import com.ayd.dto.DocumentStatusEvent;
import com.ayd.entity.Document;
import com.ayd.enums.DocumentStatus;
import com.ayd.enums.ProcessingStatus;
import com.ayd.repository.DocumentRepository;
import com.ayd.service.DocumentProcessorService;
import com.ayd.service.DocumentStatusPublisher;
import com.ayd.service.TextExtractionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
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
@ConditionalOnProperty(name = "application.ai-mode", havingValue = "true")
public class AIDocumentProcessorServiceImpl implements DocumentProcessorService {

    private final DocumentRepository documentRepository;
    private final TextExtractionService textExtractionService;
    private final TokenTextSplitter tokenTextSplitter;
    private final EmbeddingModel embeddingModel;
    private final ElasticsearchClient elasticsearchClient;
    private final ElasticSearchConfig elasticSearchConfig;
    private final DocumentStatusPublisher documentStatusPublisher;

    @KafkaListener(topics = "${application.kafka.document-upload-topic}", groupId = "${spring.kafka.consumer.group-id}")
    @Override
    public void processDocument(DocumentProcessingMessage message) {
        log.info("Processing document: {}", message.getDocumentId());
        // Get the document Metadata
        Document document = documentRepository.findById(message.getDocumentId())
                .orElseThrow(() -> new RuntimeException("Document not found: " + message.getDocumentId()));
        if(document.getProcessingStatus() == ProcessingStatus.COMPLETED) return;
        try {
            documentStatusPublisher.publish(new DocumentStatusEvent(message.getDocumentId(), message.getUploadedBy(), DocumentStatus.PROCESSING));
            String rawText = textExtractionService.extractTextFromFile(document);
            //Convert to Spring AI Document
            org.springframework.ai.document.Document adoc =
                    new org.springframework.ai.document.Document(rawText);
            // Metadata Injection (The Identity Tagging)
            // This ensures every chunk knows it belongs to a 'uploadedBy' and Document ID '123'
            adoc.getMetadata().put("documentId", document.getId());
            adoc.getMetadata().put("author", document.getAuthor());
            adoc.getMetadata().put("uploadedBy", document.getUploadedBy());
            adoc.getMetadata().put("type", document.getDocumentType().name());

            //Split into chunks
            List<org.springframework.ai.document.Document> chunks = tokenTextSplitter.apply(List.of(adoc));
            List<org.springframework.ai.document.Document> enrichedChunks = new ArrayList<>();

            int chunkIndex = 0;
            for (org.springframework.ai.document.Document chunk : chunks) {
                String newText = "Title: " + document.getTitle() + "\n\n" + chunk.getText();
                Map<String, Object> metadata = new HashMap<>(chunk.getMetadata());
                metadata.put("documentId", document.getId());
                metadata.put("chunkIndex", chunkIndex);
                metadata.put("chunkId", document.getId() + "_" + chunkIndex);
                metadata.put("title", document.getTitle());
                metadata.put("author", document.getAuthor());
                metadata.put("uploadedBy", document.getUploadedBy());
                metadata.put("type", document.getDocumentType().name());
                enrichedChunks.add(new org.springframework.ai.document.Document(newText, metadata));
                chunkIndex++;
            }
            documentStatusPublisher.publish(new DocumentStatusEvent(message.getDocumentId(), message.getUploadedBy(), DocumentStatus.EMBEDDING));
            BulkRequest.Builder br = new BulkRequest.Builder();
            for (org.springframework.ai.document.Document chunk : enrichedChunks) {
                float[] embedding = embeddingModel.embed(chunk.getText());
                log.info("Embedding dimension: {}", embedding.length);
                Map<String, Object> json = new HashMap<>();
                json.put("content", chunk.getText());
                json.put("embedding", embedding);
                json.putAll(chunk.getMetadata());
                br.operations(op -> op
                        .index(idx -> idx
                                .index(elasticSearchConfig.getIndexName())
                                .id((String) chunk.getMetadata().get("chunkId"))
                                .document(json)
                        )
                );
            }
            elasticsearchClient.bulk(br.build());
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
