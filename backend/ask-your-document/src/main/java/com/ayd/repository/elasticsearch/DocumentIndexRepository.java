package com.ayd.repository.elasticsearch;

import com.ayd.entity.elasticsearch.DocumentIndex;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

/**
 * Elasticsearch repository for document indexing and searching.
 */
@Repository
@ConditionalOnProperty(name = "application.ai-mode", havingValue = "false")
public interface DocumentIndexRepository extends ElasticsearchRepository<DocumentIndex, String> {

    /**
     * Find documents by database ID.
     *
     * @param databaseId The database ID
     * @return The matching document
     */
    DocumentIndex findByDatabaseId(Long databaseId);
}