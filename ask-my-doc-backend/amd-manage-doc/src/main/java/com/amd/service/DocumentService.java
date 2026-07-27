package com.amd.service;

import com.amd.dto.DocumentDTO;
import com.amd.enums.DocumentType;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface DocumentService {
    
    /**
     * Upload a new document
     * @param file The document file
     * @param title Document title
     * @param author Document author
     * @param username Username of the uploader
     * @return The uploaded document details
     */
    DocumentDTO uploadDocument(MultipartFile file, String title, String author, String username);
    
    /**
     * Get a document by ID
     * @param id Document ID
     * @return The document details
     */
    DocumentDTO getDocumentById(Long id);
    
    /**
     * Delete a document by ID
     * @param id Document ID
     * @param username Username of the requester (for authorization)
     */
    void deleteDocument(Long id, String username);
    
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