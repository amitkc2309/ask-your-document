package com.amd.service;

import com.amd.dto.DocumentStatusEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DocumentStatusSubscriber implements MessageListener {

    private final ObjectMapper mapper;
    private final NotificationHub notificationHub;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            DocumentStatusEvent event = mapper.readValue(message.getBody(), DocumentStatusEvent.class);
            notificationHub.publish(event);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
