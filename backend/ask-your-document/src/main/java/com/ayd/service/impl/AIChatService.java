package com.ayd.service.impl;

import com.ayd.dto.ChatRequest;
import com.ayd.dto.RerankedDocument;
import com.ayd.entity.ChatSessions;
import com.ayd.repository.ChatSessionRepository;
import com.ayd.security.SecurityUtils;
import com.ayd.service.*;
import com.ayd.utils.GenericUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AIChatService {

    @Value("${application.ai-input-size}")
    public int aiInputSize;
    @Value("${application.vector-store.top-k}")
    public int vectorStoreTopK;
    @Value("${application.vector-store.similarity-threshold}")
    public double vectorStoreSimilarityThreshold;
    private final QueryTransformationService queryTransformer;
    @Value("${application.reranker.minScoreThreshold}")
    public double minScoreThreshold;
    private final ReRankingService reRankingService;
    private final ChatClientFactory chatClientFactory;
    private final ChatOptionsFactory  chatOptionsFactory;
    @Value("classpath:/templates/SystemPromptTemplate.st")
    Resource askAnswerSystemPrompt;
    @Value("${application.query-transform}")
    private Boolean queryTransform;
    private final VectorStore vectorStore;
    private final ChatMemory chatMemory;
    private final ChatSessionRepository chatSessionRepository;

    public Flux<String> chat(ChatRequest request, String username) {
        String userQuery = request.getKeyword();
        if (request.getConversationId() == null) {
            throw new IllegalArgumentException("ChatSessionId is required");
        }
        log.info("streamSearch username:{}", username);
        //Optimize the query using LLM. TODO
        if (queryTransform)
            userQuery = queryTransformer.transform(request);

        // Execute the Search in vectorDB
        long esStartTime = System.currentTimeMillis();
        Filter.Expression filter =
                new Filter.Expression(
                        Filter.ExpressionType.EQ,
                        new Filter.Key("uploadedBy"),
                        new Filter.Value(username)
                );
        SearchRequest sr = SearchRequest.builder()
                .query(userQuery.toLowerCase())
                .topK(vectorStoreTopK)
                .filterExpression(filter)
                .similarityThreshold(vectorStoreSimilarityThreshold)
                .build();
        List<Document> vectorSearchResult = vectorStore.similaritySearch(sr);
        if (vectorSearchResult.isEmpty()) {
            return Flux.just("No results found.");
        }
        long esEndTime = System.currentTimeMillis();
        log.info("***vectorSearchResult response time: {} ms", (esEndTime-esStartTime));
        // Re-rank
        long rrStartTime = System.currentTimeMillis();
        List<RerankedDocument> reranked = reRankingService.rerank(userQuery.toLowerCase(), vectorSearchResult);
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

        long aiStartTime = System.currentTimeMillis();
        // Call to LLM
        String finalUserQuery = userQuery;
        ChatClient chatClient = chatClientFactory.getChatClient(request.getAiRequest());
        return chatClient.prompt()
                .options(chatOptionsFactory.buildChatOptions(request.getAiRequest()))
                .advisors(advisorSpec ->
                        advisorSpec
                                .advisors(MessageChatMemoryAdvisor.builder(chatMemory)
                                        .build())
                                .param(ChatMemory.CONVERSATION_ID, request.getConversationId()))
                .user(finalUserQuery)
                .system(promptUserSpec ->
                        promptUserSpec
                                .text(askAnswerSystemPrompt)
                                .param("context", context)
                )
                .stream()
                .content()
                .doOnComplete(() -> {
                    long aiEndTime = System.currentTimeMillis();
                    log.info("***AI total response time: {} ms", (aiEndTime - aiStartTime));
                });
    }

    public List<Message> getConversationById(String conversationId) {
        return chatMemory.get(conversationId);
    }

    public String createChatSessionForUser() {
        ChatSessions chatSessions = new ChatSessions();
        chatSessions.setUsername(SecurityUtils.getUsername());
        UUID conservationId = UUID.randomUUID();
        chatSessions.setConversationId(conservationId.toString());
        chatSessionRepository.save(chatSessions);
        return conservationId.toString();
    }

    public List<ChatSessions> getAllChatSessionsForUser() {
        return chatSessionRepository.findAllByUsername(SecurityUtils.getUsername());
    }
}
