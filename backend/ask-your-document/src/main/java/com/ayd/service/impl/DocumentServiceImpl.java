package com.ayd.service.impl;

import com.ayd.config.KafkaConfig;
import com.ayd.config.MinioConfig;
import com.ayd.dto.DocumentDTO;
import com.ayd.dto.DocumentProcessingMessage;
import com.ayd.dto.DocumentStatusEvent;
import com.ayd.entity.UserDocument;
import com.ayd.enums.DocumentStatus;
import com.ayd.enums.DocumentType;
import com.ayd.enums.ProcessingStatus;
import com.ayd.exception.ActionNotPermittedException;
import com.ayd.exception.ResourceNotFoundException;
import com.ayd.repository.DocumentRepository;
import com.ayd.security.SecurityUtils;
import com.ayd.service.DBServices;
import com.ayd.service.DocumentService;
import com.ayd.service.DocumentStatusPublisher;
import com.ayd.service.ObjectStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentServiceImpl implements DocumentService {

    private final DocumentRepository documentRepository;
    private final ObjectStorageService storageService;
    private final KafkaTemplate<String, DocumentProcessingMessage> kafkaTemplate;
    private final DBServices dbServices;
    private final MinioConfig minioConfig;
    private final KafkaConfig kafkaConfig;
    private final DocumentStatusPublisher  documentStatusPublisher;

    @Override
    public DocumentDTO uploadDocument(MultipartFile file, String title, String author, String userId) {

        // Determine document type
        DocumentType documentType = determineDocumentType(file.getOriginalFilename());
        // Create document entity with PENDING status
        UserDocument document = UserDocument.builder()
                .title(title)
                .fileName(file.getOriginalFilename())
                .contentType(file.getContentType())
                .fileSize(file.getSize())
                .author(author)
                .uploadDate(LocalDateTime.now())
                .uploadedBy(userId)
                .documentType(documentType)
                .processingStatus(ProcessingStatus.PENDING)
                .build();

        // Save document metadata to get an ID
        UserDocument savedDocument = documentRepository.save(document);
        String objectId = userId + minioConfig.getMinioFileDelimiter() + savedDocument.getId();
        documentStatusPublisher.publish(new DocumentStatusEvent(savedDocument.getId(), userId, DocumentStatus.UPLOADING));
        // Store the file in the file system
        storageService.upload(objectId, file);
        documentStatusPublisher.publish(new DocumentStatusEvent(savedDocument.getId(), userId, DocumentStatus.UPLOADED));
        // Update the document with the file path
        savedDocument.setFilePath(objectId);
        savedDocument = documentRepository.save(savedDocument);

        // Create a message for Kafka
        DocumentProcessingMessage message = DocumentProcessingMessage.builder()
                .documentId(savedDocument.getId())
                .fileName(savedDocument.getFileName())
                .contentType(savedDocument.getContentType())
                .filePath(savedDocument.getFilePath())
                .title(savedDocument.getTitle())
                .author(savedDocument.getAuthor())
                .uploadedBy(savedDocument.getUploadedBy())
                .uploadDate(savedDocument.getUploadDate())
                .build();

        // Send the message to Kafka for asynchronous processing
        try {
            kafkaTemplate.send(kafkaConfig.getDocumentUploadTopic(), savedDocument.getId().toString(), message);
            log.info("Document processing message sent successfully: {}", savedDocument.getId());
            documentStatusPublisher.publish(new DocumentStatusEvent(savedDocument.getId(), userId, DocumentStatus.QUEUED));
        } catch (Exception ex) {
            log.error("Failed to send document processing message: {}", ex.getMessage(), ex);
            // Update document status to FAILED
            savedDocument.setProcessingStatus(ProcessingStatus.FAILED);
            documentRepository.save(savedDocument);
            documentStatusPublisher.publish(new DocumentStatusEvent(savedDocument.getId(), userId, DocumentStatus.FAILED));
        }

        log.info("Document uploaded and queued for processing: {}", savedDocument.getId());

        // Return DTO
        return mapToDTO(savedDocument);
    }

    @Override
    @Transactional(readOnly = true)
    public DocumentDTO getDocumentById(Long id) {
        log.debug("Fetching document by ID: {}", id);
        UserDocument document = documentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Document", "id", id));
        String userId = SecurityUtils.getUserId();
        if (!document.getUploadedBy().equals(userId)) {
            throw new ActionNotPermittedException(userId, "Document", id);
        }
        return mapToDTO(document);
    }

    @Override
    @Transactional
    public void deleteDocument(Long id, String userId) {
        log.info("Deleting document. ID: {}, userId: {}", id, userId);
        UserDocument document = documentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Document", "id", id));
        if (!document.getUploadedBy().equals(userId)) {
            throw new ActionNotPermittedException(userId, "Document", id);
        }
        try {
            // Delete from or VectorDB first
            dbServices.deleteByDocumentId(id);
            // Delete the file from storage
            if (document.getFilePath() != null) {
                storageService.delete(document.getFilePath());
            }
            // Delete from database
            documentRepository.delete(document);
            log.info("Document deleted successfully. ID: {}", id);
        } catch (Exception e) {
            log.error("Error deleting document. ID: {}", id, e);
            throw new RuntimeException("Failed to delete document completely", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<DocumentDTO> findByAuthor(String author) {
        log.debug("Searching documents by author: {}, Page: {}", author);
        return documentRepository.findByAuthorContainingIgnoreCaseAndUploadedBy(author,SecurityUtils.getUserId())
                .stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<DocumentDTO> findByTitle(String title) {
        log.debug("Searching documents by title: {}, Page: {}", title);
        return documentRepository
                .findByTitleContainingIgnoreCaseAndUploadedBy(title,SecurityUtils.getUserId())
                .stream().map(this::mapToDTO).collect(Collectors.toList());

    }

    @Override
    public List<DocumentDTO> getAllDocument() {
        return documentRepository
                .findByUploadedBy(SecurityUtils.getUserId())
                .stream().map(this::mapToDTO).collect(Collectors.toList());

    }

    @Override
    @Transactional(readOnly = true)
    public List<DocumentDTO> findByDocumentType(DocumentType documentType) {
        log.debug("Searching documents by type: {}, Page: {}", documentType);
        return documentRepository.findByDocumentTypeAndUploadedBy(documentType, SecurityUtils.getUserId())
                .stream().map(this::mapToDTO).collect(Collectors.toList());
    }


    private DocumentDTO mapToDTO(UserDocument document) {
        return DocumentDTO.builder()
                .id(document.getId())
                .title(document.getTitle())
                .fileName(document.getFileName())
                .contentType(document.getContentType())
                .fileSize(document.getFileSize())
                .author(document.getAuthor())
                .filePath(document.getFilePath())
                .uploadDate(document.getUploadDate())
                .lastModifiedDate(document.getLastModifiedDate())
                .uploadedBy(document.getUploadedBy())
                .documentType(document.getDocumentType())
                .build();
    }

    private DocumentType determineDocumentType(String fileName) {
        if (fileName == null) {
            return DocumentType.OTHER;
        }

        String extension = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();

        switch (extension) {
            case "pdf":
                return DocumentType.PDF;
            case "docx":
            case "doc":
                return DocumentType.DOCX;
            case "txt":
                return DocumentType.TXT;
            case "rtf":
                return DocumentType.RTF;
            default:
                return DocumentType.OTHER;
        }
    }
}
