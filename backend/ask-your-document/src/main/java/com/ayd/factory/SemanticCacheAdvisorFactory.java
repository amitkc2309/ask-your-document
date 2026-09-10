package com.ayd.factory;

import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;
import org.springframework.ai.chat.cache.semantic.SemanticCache;
import org.springframework.ai.chat.cache.semantic.SemanticCacheAdvisor;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.redis.cache.semantic.DefaultSemanticCache;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import redis.clients.jedis.RedisClient;

@Component
@RequiredArgsConstructor
@Log
public class SemanticCacheAdvisorFactory {

    @Qualifier("semanticCacheRedisClient")
    private final RedisClient semanticCacheRedisClient;

    private final EmbeddingModel embeddingModel;

    @Value("${application.semantic-cache.similarity-threshold}")
    private Double semanticCacheSimilarityThreshold;

    public SemanticCacheAdvisor createSemanticCacheAdvisor(String username) {
        if (username == null) {
            throw new IllegalArgumentException("username can not be null");
        }
        return SemanticCacheAdvisor.builder()
                .cache(semanticCache(username))
                .build();

    }

    private SemanticCache semanticCache(String username) {
        log.info("**********semanticCache  start**********"+username);
        String indexName = username + "-semantic-cache";
        String prefix = "ayd-semantic-cache:" + username + ":";
        return DefaultSemanticCache.builder()
                .jedisClient(semanticCacheRedisClient)
                .embeddingModel(embeddingModel)
                .similarityThreshold(this.semanticCacheSimilarityThreshold)
                .indexName(indexName)
                .prefix(prefix)
                .build();
    }
}
