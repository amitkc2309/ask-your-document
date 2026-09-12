package com.ayd.service;

import com.ayd.dto.DocumentDTO;
import com.ayd.enums.DocumentType;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface DocumentService {
    
    /**
     * Upload a new document
     * @param file The document file
     * @param title Document title
     * @param author Document author
     * @param userId userId of the uploader
     * @return The uploaded document details
     */
    DocumentDTO uploadDocument(MultipartFile file, String title, String author, String userId);
    
    /**
     * Get a document by ID
     * @param id Document ID
     * @return The document details
     */
    DocumentDTO getDocumentById(Long id);
    
    /**
     * Delete a document by ID
     * @param id Document ID
     * @param userId of the requester (for authorization)
     */
    void deleteDocument(Long id, String userId);
    
    /**
     * Search documents by author
     * @param author Author name
     * @return List of documents by the author
     */
    List<DocumentDTO> findByAuthor(String author);
    
    /**
     * Search documents by title
     * @param title Document title
     * @return List of documents with matching title
     */
    List<DocumentDTO> findByTitle(String title);
    
    /**
     * Search documents by document type
     * @param documentType Document type
     * @return List of documents of the specified type
     */
    List<DocumentDTO> findByDocumentType(DocumentType documentType);

    List<DocumentDTO> getAllDocument();
}