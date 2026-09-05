package com.ayd.service.impl;

import com.ayd.entity.elasticsearch.DocumentIndex;
import com.ayd.repository.elasticsearch.DocumentIndexRepository;
import com.ayd.service.DBServices;
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
