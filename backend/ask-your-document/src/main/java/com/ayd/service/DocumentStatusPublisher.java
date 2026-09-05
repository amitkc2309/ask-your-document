package com.ayd.service;

import com.ayd.dto.DocumentStatusEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DocumentStatusPublisher {

    private final RedisTemplate<String, Object> redisTemplate;
    @Qualifier("documentStatusTopic")
    private final ChannelTopic documentStatusTopic;

    public void publish(DocumentStatusEvent event) {
        redisTemplate.convertAndSend(
                documentStatusTopic.getTopic(),
                event
        );
    }
}
