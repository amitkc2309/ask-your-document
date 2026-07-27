package com.amd.service;

import com.amd.dto.RerankRequest;
import com.amd.dto.RerankRequestItem;
import com.amd.dto.RerankResponse;
import com.amd.dto.RerankedDocument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "application.ai-mode", havingValue = "true")
public class ReRankingService {
    private final WebClient webClient;

    @Value("${application.reranker.reranker-url}")
    private String rerankerUrl;

    public List<RerankedDocument> rerank(String query, List<Document> documents) {
        if (documents == null || documents.isEmpty()) {
            return List.of();
        }
        List<RerankRequestItem> items = documents.stream()
                .map(d -> new RerankRequestItem(
                        (String) d.getMetadata().get("chunkId"),
                        d.getText()
                ))
                .toList();
        RerankRequest request = new RerankRequest(query, items);
        List<RerankResponse> response = webClient
                .post()
                .uri(rerankerUrl + "/rerank")
                .bodyValue(request)
                .retrieve()
                .bodyToFlux(RerankResponse.class)
                .collectList()
                .block();
        if (response == null || response.isEmpty()) {
            log.warn("Reranker returned empty response");
            return documents.stream()
                    .map(d -> new RerankedDocument(d, 0.0))
                    .toList();
        }
        Map<String, Document> docMap = documents.stream()
                .collect(Collectors.toMap(
                        d -> (String) d.getMetadata().get("chunkId"),
                        d -> d
                ));

        return response.stream()
                .map(r -> new RerankedDocument(
                        docMap.get(r.getId()),
                        r.getScore()
                ))
                .filter(r -> r.document() != null)
                .sorted(Comparator.comparing(RerankedDocument::score).reversed())
                .toList();
    }
}