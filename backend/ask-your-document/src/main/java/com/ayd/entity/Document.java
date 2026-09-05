package com.ayd.entity;

import com.ayd.enums.DocumentType;
import com.ayd.enums.ProcessingStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "documents")
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String fileName;

    @Column(nullable = false)
    private String contentType;

    @Column(nullable = false)
    private Long fileSize;

    @Column(nullable = false)
    private String author;

    @Column
    private String filePath;

    @Column
    private String elasticsearchId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProcessingStatus processingStatus = ProcessingStatus.PENDING;

    @Column(nullable = false)
    private LocalDateTime uploadDate;

    @Column
    private LocalDateTime lastModifiedDate;

    @Column
    private LocalDateTime processedDate;

    @Column
    private String uploadedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DocumentType documentType;
}
