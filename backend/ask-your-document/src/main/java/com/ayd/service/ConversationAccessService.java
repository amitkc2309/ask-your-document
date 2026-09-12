package com.ayd.service;

import com.ayd.exception.ActionNotPermittedException;
import com.ayd.repository.ChatSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
@Log
public class ConversationAccessService {

    private final RedisTemplate<String, String> redisTemplate;
    private final ChatSessionRepository chatSessionRepository;

    private static final String CACHE_PREFIX = "conversation-access:";

    public void validateConversationAccess(String conversationId, String userId) {
        String cacheKey = CACHE_PREFIX + userId + ":" + conversationId;
        Boolean cached = redisTemplate.hasKey(cacheKey);
        if (Boolean.TRUE.equals(cached)) {
            return;
        }
        boolean exists = chatSessionRepository.existsByUserIdAndConversationId(userId, conversationId);
        if (!exists) {
            throw new ActionNotPermittedException(
                    "User", "Conversation", "");
        }
        redisTemplate.opsForValue().set(
                cacheKey,
                "true",
                Duration.ofMinutes(60)
        );
    }

    public void invalidateConversationAccess(String conversationId, String userId) {
        String cacheKey = CACHE_PREFIX + userId + ":" + conversationId;
        redisTemplate.delete(cacheKey);
    }
}
