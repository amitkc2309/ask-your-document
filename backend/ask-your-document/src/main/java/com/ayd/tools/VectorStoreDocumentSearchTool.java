package com.ayd.tools;

import com.ayd.service.VectorStoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.document.Document;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Log
@RequiredArgsConstructor
public class VectorStoreDocumentSearchTool {

   private final VectorStoreService vectorStoreService;

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
        String userId = (String) toolContext.getContext().get("userId");
        if (userId == null || userId.isBlank()) {
            throw new IllegalStateException("Missing userId in tool context — refusing to search");
        }
        log.info("userId during vector_db_search tool: " + userId);
        return vectorStoreService.search(userId, semanticSearchQuery);
    }
}
