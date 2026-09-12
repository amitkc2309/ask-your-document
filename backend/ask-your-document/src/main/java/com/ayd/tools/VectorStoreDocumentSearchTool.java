package com.ayd.tools;

import com.ayd.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.document.Document;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Log
@RequiredArgsConstructor
public class VectorStoreDocumentSearchTool {

    private final VectorStore vectorStore;
    @Value("${application.vector-store.top-k}")
    Integer topK;
    @Value("${application.vector-store.similarity-threshold}")
    Double similarityThreshold;

    @Tool(name="vector_db_search",
    description = """
    Search the Vector Database for information needed to answer the user's query. Use this when the answer may
    depend on information contained in the user's documents. Do not use this tool for greetings, casual conversation.
    Before calling this tool synthesize the semanticSearchQuery from conversation history and users's query to provide 
    better result. Ensure the semanticSearchQuery query is clear, specific, and maintains the user's intent.
    """)
    public List<Document> vectorDBSearch(@ToolParam(description = "The semanticSearchQuery to search in Vector Database")
                                             String semanticSearchQuery,
                                         ToolContext toolContext) {
        log.info("Searching for documents using semanticSearchQuery=>"+semanticSearchQuery);
        String username = (String) toolContext.getContext().get("username");
        if (username == null || username.isBlank()) {
            throw new IllegalStateException("Missing username in tool context — refusing to search");
        }
        log.info("username during vector_db_search tool: " + username);
        Filter.Expression vectorDBSearchFilter = new Filter.Expression(
                Filter.ExpressionType.EQ,
                new Filter.Key("uploadedBy"),
                new Filter.Value(username)
        );
        SearchRequest sr = SearchRequest.builder()
                .query(semanticSearchQuery)
                .topK(topK)
                .similarityThreshold(similarityThreshold)
                .filterExpression(vectorDBSearchFilter)
                .build();
        return vectorStore.similaritySearch(sr);
    }
}
