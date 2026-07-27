package com.amd.advisors;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import reactor.core.publisher.Flux;

import java.util.concurrent.atomic.AtomicReference;

@Slf4j
public class TokenUsageAuditAdvisor implements StreamAdvisor {
    @Override
    public String getName() {
        return "TokenUsageAuditAdvisor";
    }

    @Override
    public int getOrder() {
        return 1;
    }

    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest chatClientRequest, StreamAdvisorChain streamAdvisorChain) {
        return streamAdvisorChain.nextStream(chatClientRequest)
                .doOnNext(chatClientResponse -> {
                    ChatResponse chatResponse = chatClientResponse.chatResponse();
                    if (chatResponse != null && chatResponse.getMetadata() != null && !chatResponse.getMetadata().isEmpty()) {
                        Usage usage = chatResponse.getMetadata().getUsage();
                        if (usage != null && usage.getTotalTokens() > 0) {
                            log.info("Token Usage Details {}", usage);
                        }
                    }
                });
    }
}
