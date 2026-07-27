package com.amd.service.impl;

import com.amd.entity.elasticsearch.DocumentIndex;
import com.amd.repository.elasticsearch.DocumentIndexRepository;
import com.amd.service.DBServices;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "application.ai-mode", havingValue = "false")
public class ElasticSearchDBService implements DBServices {

    private final DocumentIndexRepository repository;

    @Override
    public void deleteByDocumentId(Long id) {
        DocumentIndex doc = repository.findByDatabaseId(id);
        if (doc != null) {
            repository.delete(doc);
        }
    }
}
