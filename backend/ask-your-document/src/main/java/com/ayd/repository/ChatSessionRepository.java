package com.ayd.repository;

import com.ayd.entity.ChatSessions;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatSessionRepository extends JpaRepository<ChatSessions, Long> {
    List<ChatSessions> findAllByUserId(String userId);
    void deleteByConversationId(String conversationId);
    boolean existsByUserIdAndConversationId(String userId, String conversationId);
}
