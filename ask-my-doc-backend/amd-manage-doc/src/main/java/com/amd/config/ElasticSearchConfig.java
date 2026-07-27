package com.amd.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.mapping.DenseVectorIndexOptions;
import co.elastic.clients.elasticsearch._types.mapping.DenseVectorIndexOptionsType;
import co.elastic.clients.elasticsearch._types.mapping.DenseVectorSimilarity;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "application.ai-mode", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class ElasticSearchConfig {

    @Getter
    @Value("${application.elasticsearch.index-name}")
    private String indexName;
    private final ElasticsearchClient elasticsearchClient;
    private final EmbeddingModel embeddingModel;

    @Bean
    public ApplicationRunner createIndex() {
        log.info("Embedding dimension at create Index time: {}", embeddingModel.dimensions());
        return args -> {
            boolean exists = elasticsearchClient.indices()
                    .exists(e -> e.index(indexName))
                    .value();
            if (!exists) {
                elasticsearchClient.indices()
                        .create(c -> c
                        .index(indexName)
                        .mappings(m -> m
                                .properties("content", p -> p.text(t -> t))
                                .properties("embedding", p -> p
                                        .denseVector(d -> d
                                                .dims(embeddingModel.dimensions())
                                                .index(true)
                                                .similarity(DenseVectorSimilarity.Cosine)
                                                .indexOptions(DenseVectorIndexOptions.of(o -> o
                                                        .type(DenseVectorIndexOptionsType.Hnsw)
                                                        .m(16)
                                                        .efConstruction(100)
                                                ))
                                        )
                                )
                                .properties("documentId", p -> p.keyword(k -> k))
                                .properties("chunkId", p -> p.keyword(k -> k))
                                .properties("uploadedBy", p -> p.keyword(k -> k))
                                .properties("title", p -> p
                                        .text(t -> t
                                                .fields("keyword", k -> k.keyword(kd -> kd))
                                        )
                                )
                        )
                );
            }
        };
    }
}
