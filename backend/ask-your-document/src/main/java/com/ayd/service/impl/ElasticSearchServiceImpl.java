package com.ayd.service.impl;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.MultiMatchQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import com.ayd.dto.ChatRequest;
import com.ayd.dto.QuestionResponse;
import com.ayd.dto.QuestionResponse.DocumentSnippet;
import com.ayd.entity.elasticsearch.DocumentIndex;
import com.ayd.security.SecurityUtils;
import com.ayd.service.SearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.HighlightQuery;
import org.springframework.data.elasticsearch.core.query.highlight.Highlight;
import org.springframework.data.elasticsearch.core.query.highlight.HighlightField;
import org.springframework.data.elasticsearch.core.query.highlight.HighlightFieldParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "application.ai-mode", havingValue = "false")
public class ElasticSearchServiceImpl implements SearchService {

    private final ElasticsearchOperations elasticsearchOperations;

    @Override
    @Transactional
    public QuestionResponse search(ChatRequest request) {
        log.info("Processing search for: {}", request.getKeyword());

        Query multiMatchQuery = MultiMatchQuery.of(m -> m
                .query(request.getKeyword())
                .fields("title^3", "author^2", "content")
                .fuzziness("AUTO")
                .prefixLength(3)
                .maxExpansions(10)
        )._toQuery();

        Query securityFilterQuery = BoolQuery.of(b -> b
                .filter(f -> f.term(t -> t
                        .field("uploadedBy")
                        .value(SecurityUtils.getUsername())
                ))
        )._toQuery();

        HighlightFieldParameters parameters = HighlightFieldParameters.builder()
                .withFragmentSize(120)
                .withNumberOfFragments(1)
                .withPreTags("<span class='search-highlight'>")
                .withPostTags("</span>")
                .build();

        HighlightField highlightField =
                new HighlightField("content", parameters);

        Highlight highlight =
                new Highlight(List.of(highlightField));
        // Create NativeQuery using the query above
        NativeQuery searchQuery = new NativeQueryBuilder()
                .withQuery(multiMatchQuery)
                .withFilter(securityFilterQuery)
                .withHighlightQuery(
                        new HighlightQuery(
                                highlight,
                                DocumentIndex.class
                        )
                )
                .build();

        // Execute search using Spring Data Elasticsearch
        SearchHits<DocumentIndex> searchHits = elasticsearchOperations.search(searchQuery, DocumentIndex.class);
        // Convert hits to snippet DTOs using score from Elasticsearch
        List<DocumentSnippet> snippets = searchHits.getSearchHits().stream()
                .map(hit -> {
                    String snippet = "";
                    List<String> highlights = hit.getHighlightFields().get("content");
                    if (highlights != null && !highlights.isEmpty()) {
                        snippet = highlights.get(0);
                    }
                    return DocumentSnippet.builder()
                            .documentId(hit.getContent().getDatabaseId())
                            .documentTitle(hit.getContent().getTitle())
                            .author(hit.getContent().getAuthor())
                            .snippet(snippet)
                            .relevanceScore((double) hit.getScore())
                            .build();
                })
                .limit(request.getMaxResults() != null ? request.getMaxResults() : Integer.MAX_VALUE)
                .toList();

        return QuestionResponse.builder()
                .question(request.getKeyword())
                .snippets(snippets)
                .totalResults((int) searchHits.getTotalHits())
                .build();
    }

    @Override
    public Flux<String> chat(ChatRequest request, String username) {
        throw new UnsupportedOperationException(
                "Chat is not supported. Use search() instead.");
    }
}
