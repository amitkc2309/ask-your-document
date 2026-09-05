package com.ayd.service.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.DeleteByQueryRequest;
import com.ayd.config.ElasticSearchConfig;
import com.ayd.service.DBServices;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "application.ai-mode", havingValue = "true")
@Slf4j
public class VectorDBServices implements DBServices {

    private final ElasticSearchConfig elasticSearchConfig;
    private final ElasticsearchClient elasticSearchClient;

    @Override
    public void deleteByDocumentId(Long documentId) {
        try {
            DeleteByQueryRequest request = new DeleteByQueryRequest.Builder()
                    .index(elasticSearchConfig.getIndexName())
                    .refresh(true)
                    .query(q -> q
                            .term(t -> t
                                    .field("documentId")
                                    .value(String.valueOf(documentId))
                            )
                    )
                    .build();
            elasticSearchClient.deleteByQuery(request);
            log.info("Deleted all chunks for documentId={}", documentId);

        } catch (Exception e) {
            log.error("Failed to delete documentId={}", documentId, e);
            throw new RuntimeException(e);
        }
    }
}
