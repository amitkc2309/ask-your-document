package com.amd.repository;

import com.amd.entity.Document;
import com.amd.enums.DocumentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {

    // Find documents by author
    List<Document> findByAuthorContainingIgnoreCase(String author);

    List<Document> findByAuthorContainingIgnoreCaseAndUploadedBy(String author, String uploadedBy);

    // Find documents by title
    List<Document> findByTitleContainingIgnoreCase(String title);
    List<Document> findByTitleContainingIgnoreCaseAndUploadedBy(String title, String uploadedBy);

    // Find documents by document type
    List<Document> findByDocumentType(DocumentType documentType);
    List<Document> findByDocumentTypeAndUploadedBy(DocumentType documentType, String uploadedBy);

    // Find documents by uploader
    List<Document> findByUploadedBy(String uploadedBy);
}
