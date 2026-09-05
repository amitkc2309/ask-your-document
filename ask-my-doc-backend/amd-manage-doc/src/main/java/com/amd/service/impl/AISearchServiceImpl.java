package com.amd.service.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import com.amd.config.ElasticSearchConfig;
import com.amd.dto.QuestionResponse;
import com.amd.dto.RerankedDocument;
import com.amd.dto.ChatRequest;
import com.amd.service.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "application.ai-mode", havingValue = "true")
public class AISearchServiceImpl implements SearchService {

    @Value("${application.elasticsearch.rrf-limit}")
    public int rrfLimit;
    @Value("${application.ai-input-size}")
    public int aiInputSize;
    @Value("${application.elasticsearch.bm25-size}")
    public int bm25Size;
    @Value("${application.elasticsearch.vector-size}")
    public int vectorSize;
    @Value("${application.elasticsearch.vector-k}")
    public int vectorK;
    @Value("${application.elasticsearch.vector-num-candidate}")
    public int vectorNumCandidate;
    private final QueryTransformationService queryTransformer;
    @Value("${application.reranker.minScoreThreshold}")
    public double minScoreThreshold;
    private final ReRankingService reRankingService;
    private final ChatClientFactory chatClientFactory;
    private final ChatOptionsFactory  chatOptionsFactory;
    @Value("classpath:/templates/UserPromptTemplate.st")
    Resource askAnswerUserPrompt;
    @Value("${application.query-transform}")
    private Boolean queryTransform;
    private final EmbeddingModel embeddingModel;
    private final ElasticsearchClient elasticsearchClient;
    private final ElasticSearchConfig elasticSearchConfig;
    private final ChatMemory chatMemory;

    public Flux<String> chat(ChatRequest request, String username) {
        String userQuery = request.getKeyword();
        log.info("streamSearch username:{}", username);
        //Optimize the query using LLM. TODO
        if (queryTransform)
            userQuery = queryTransformer.transform(request);

        // Execute the Search in vectorDB
        long esStartTime = System.currentTimeMillis();
        List<Document> elasticRRFResult = elasticHybridSearch(userQuery.toLowerCase(), username);
        if (elasticRRFResult.isEmpty()) {
            return Flux.just("No results found.");
        }
        long esEndTime = System.currentTimeMillis();
        log.info("***ElasticSearch response time: {} ms", (esEndTime-esStartTime));
        // Re-rank
        long rrStartTime = System.currentTimeMillis();
        List<RerankedDocument> reranked = reRankingService.rerank(userQuery.toLowerCase(), elasticRRFResult);
        long rrEndTime = System.currentTimeMillis();
        log.info("***RankingService response time: {} ms", (rrEndTime-rrStartTime));
        double maxScore = reranked.get(0).score();

        if (maxScore < minScoreThreshold) {
            return Flux.just("No relevant documents found.");
        }

        List<RerankedDocument> filteredByRank = reranked.stream()
                .limit(aiInputSize)
                .toList();
        // Create context for AI with relevant document
        String context = filteredByRank.stream()
                .map(RerankedDocument::document)
                .map(doc -> {
                    return doc.getText();
                })
                .collect(Collectors.joining("\n\n---\n\n"));

        List<QuestionResponse.DocumentSnippet> snippets = createSnippets(request, filteredByRank);
        try {
            String json = new ObjectMapper().writeValueAsString(snippets);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        long aiStartTime = System.currentTimeMillis();
        // Call to LLM
        String finalUserQuery = userQuery;
        ChatClient chatClient = chatClientFactory.getChatClient(request.getAiRequest());
        return chatClient.prompt()
                .options(chatOptionsFactory.buildChatOptions(request.getAiRequest()))
                .advisors(advisorSpec ->
                        advisorSpec
                                .advisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                                //username will work as conversationID
                                .param(ChatMemory.CONVERSATION_ID, username))
                .user(promptUserSpec ->
                        promptUserSpec
                                .text(askAnswerUserPrompt)
                                .param("query", finalUserQuery)
                                .param("context", context)
                )
                .stream()
                .content()
                .doOnComplete(() -> {
                    long aiEndTime = System.currentTimeMillis();
                    log.info("***AI total response time: {} ms", (aiEndTime - aiStartTime));
                });
    }

    private List<QuestionResponse.DocumentSnippet> createSnippets(ChatRequest request, List<RerankedDocument> reranked) {
        List<QuestionResponse.DocumentSnippet> snippets = reranked
                .stream()
                .map(r -> {
                    Document doc = r.document();
                    return QuestionResponse.DocumentSnippet.builder()
                            .documentId(doc.getMetadata().get("id") != null
                                    ? Long.valueOf(doc.getMetadata().get("id").toString())
                                    : null)
                            .documentTitle((String) doc.getMetadata().get("title"))
                            .author((String) doc.getMetadata().get("author"))
                            .snippet(shortedSnippet(doc.getText(),request.getKeyword()))
                            .relevanceScore(r.score())
                            .build();
                })
                .limit(request.getMaxResults() != null ? request.getMaxResults() : Integer.MAX_VALUE)
                .toList();
        return snippets;
    }

    private String shortedSnippet(String text, String query) {
        if (text == null || text.isBlank()) {
            return "";
        }
        int snippetSize = 300;
        String lowerText = text.toLowerCase();
        String lowerQuery = query.toLowerCase();
        int idx = lowerText.indexOf(lowerQuery);
        String snippet;
        // keyword not found
        if (idx == -1) {
            snippet = text.substring(0, Math.min(snippetSize, text.length()));
        } else {
            int start = Math.max(0, idx - 100);
            int end = Math.min(text.length(), idx + 200);
            snippet = text.substring(start, end);
        }
        // Highlight matched words
        for (String word : query.split("\\s+")) {
            snippet = snippet.replaceAll(
                    "(?i)" + Pattern.quote(word),
                    "<span class='search-highlight'>$0</span>"
            );
        }
        return snippet;
    }

    public List<Document> elasticHybridSearch(String query, String username) {

        try {
            float[] queryVector = embeddingModel.embed(query);
            List<Float> vectorList = new ArrayList<>(queryVector.length);
            for (float v : queryVector) {
                vectorList.add(v);
            }
            List<Map> bm25 = bm25Search(query, username);
            List<Map> vector = vectorSearch(vectorList, username);

            return reciprocalRankFusion(bm25, vector);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private List<Map> bm25Search(String query, String username) throws IOException {
        SearchResponse<Map> response = elasticsearchClient.search(s -> s
                        .index(elasticSearchConfig.getIndexName())
                        .size(bm25Size)
                        .query(q -> q
                                .bool(b -> b
                                        .must(m -> m.match(mm -> mm
                                                .field("content")
                                                .query(query)
                                        ))
                                        .filter(f -> f.term(t -> t
                                                .field("uploadedBy")
                                                .value(username)
                                        ))
                                )
                        ),
                Map.class
        );
        return response.hits().hits().stream()
                .map(h -> h.source())
                .toList();
    }

    private List<Map> vectorSearch(List<Float> vector, String username) throws IOException {
        SearchResponse<Map> response = elasticsearchClient.search(s -> s
                        .index(elasticSearchConfig.getIndexName())
                        .size(vectorSize)
                        .knn(k -> k
                                .field("embedding")
                                .queryVector(vector)
                                .k(vectorK)
                                .numCandidates(vectorNumCandidate)
                                .filter(f -> f
                                        .term(t -> t
                                                .field("uploadedBy")
                                                .value(username)
                                        )
                                )
                        ),
                Map.class
        );
        return response.hits().hits().stream()
                .map(h -> h.source())
                .toList();
    }

    private List<Document> reciprocalRankFusion(List<Map> bm25Results, List<Map> vectorResults) {

        Map<String, Double> scores = new HashMap<>();
        Map<String, Map> docMap = new HashMap<>();

        int k = 60; // standard value
        // BM25 ranking
        for (int i = 0; i < bm25Results.size(); i++) {
            Map doc = bm25Results.get(i);
            String id = (String) doc.get("chunkId");

            scores.put(id, scores.getOrDefault(id, 0.0) + 1.0 / (k + i + 1));
            docMap.put(id, doc);
        }
        // Vector ranking
        for (int i = 0; i < vectorResults.size(); i++) {
            Map doc = vectorResults.get(i);
            String id = (String) doc.get("chunkId");

            scores.put(id, scores.getOrDefault(id, 0.0) + 1.0 / (k + i + 1));
            docMap.put(id, doc);
        }

        return scores.entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .limit(rrfLimit)
                .map(e -> {
                    Map src = docMap.get(e.getKey());
                    return new Document(
                            (String) src.get("content"),
                            src
                    );
                })
                .toList();
    }

    @Override
    public QuestionResponse search(ChatRequest question) {
        throw  new UnsupportedOperationException("Non-streaming search is not supported. Use chat() instead.");
    }
}
