package com.ayd.service;

import com.ayd.entity.UserDocument;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TextExtractionService {

    private final ObjectStorageService objectStorageService;

    public List<Document> extractTextFromFile(UserDocument document) throws IOException {
        if (document.getFilePath() == null) {
            return Collections.emptyList();
        }
        try (InputStream inputStream = objectStorageService.download(document.getFilePath().toLowerCase())) {
            Resource resource = new InputStreamResource(inputStream);
            TikaDocumentReader tikaDocumentReader = new TikaDocumentReader(resource);
            return tikaDocumentReader.get();
        }
    }
}
