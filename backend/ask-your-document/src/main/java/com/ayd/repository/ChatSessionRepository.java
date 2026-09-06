package com.ayd.repository;

import com.ayd.entity.ChatSessions;
import com.ayd.entity.UserDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatSessionRepository extends JpaRepository<ChatSessions, Long> {
    List<ChatSessions> findAllByUsername(String username);
    void deleteByConversationId(String conversationId);
}
