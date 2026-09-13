package com.ayd.repository;

import com.ayd.entity.UserDocument;
import com.ayd.enums.DocumentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserDocumentRepository extends JpaRepository<UserDocument, Long> {

    // Find documents by author
    List<UserDocument> findByAuthorContainingIgnoreCase(String author);

    List<UserDocument> findByAuthorContainingIgnoreCaseAndUploadedBy(String author, String uploadedBy);

    // Find documents by title
    List<UserDocument> findByTitleContainingIgnoreCase(String title);
    List<UserDocument> findByTitleContainingIgnoreCaseAndUploadedBy(String title, String uploadedBy);

    // Find documents by document type
    List<UserDocument> findByDocumentType(DocumentType documentType);
    List<UserDocument> findByDocumentTypeAndUploadedBy(DocumentType documentType, String uploadedBy);

    // Find documents by uploader
    List<UserDocument> findByUploadedBy(String uploadedBy);
}
