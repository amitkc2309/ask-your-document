package com.amd.controller;

import com.amd.dto.DocumentDTO;
import com.amd.dto.DocumentStatusEvent;
import com.amd.enums.DocumentType;
import com.amd.security.SecurityUtils;
import com.amd.service.DocumentService;
import com.amd.service.NotificationHub;
import com.amd.service.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import java.io.InputStream;
import java.util.List;

@RestController
@RequestMapping("/manage/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;
    private final StorageService storageService;
    private final NotificationHub notificationHub;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentDTO> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam("title") String title,
            @RequestParam("author") String author) {

        DocumentDTO dto = documentService.uploadDocument(file, title, author, SecurityUtils.getUsername());
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(dto);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Resource> getDocumentById(@PathVariable Long id) {
        DocumentDTO document = documentService.getDocumentById(id);

        InputStream fileInputStream = storageService.download(document.getFilePath());
        InputStreamResource resource = new InputStreamResource(fileInputStream);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(document.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + document.getFileName() + "\"")
                .body(resource);
    }

    @GetMapping(value="/{id}/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<DocumentStatusEvent> getDocumentEvents(@PathVariable Long id) {
        return notificationHub.stream(id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDocument(@PathVariable Long id) {
        documentService.deleteDocument(id, SecurityUtils.getUsername());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/by-author")
    public ResponseEntity<List<DocumentDTO>> findByAuthor(
            @RequestParam String author,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);

        List<DocumentDTO> result = documentService.findByAuthor(author);

        return ResponseEntity.ok(result);
    }

    @GetMapping("/by-title")
    public ResponseEntity<List<DocumentDTO>> findByTitle(
            @RequestParam String title,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {


        List<DocumentDTO> result = documentService.findByTitle(title);

        return ResponseEntity.ok(result);
    }


    @GetMapping("/by-type")
    public ResponseEntity<List<DocumentDTO>> findByDocumentType(
            @RequestParam DocumentType documentType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        List<DocumentDTO> result = documentService.findByDocumentType(documentType);

        return ResponseEntity.ok(result);
    }

    @GetMapping("/all")
    public ResponseEntity<List<DocumentDTO>> getAllDocuments() {
        List<DocumentDTO> result = documentService.getAllDocument();
        return ResponseEntity.ok(result);
    }

}
