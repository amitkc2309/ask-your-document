package com.ayd.repository;

import com.ayd.entity.ChatSessions;
import com.ayd.entity.UserDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatSessionRepository extends JpaRepository<ChatSessions, Long> {
    List<ChatSessions> findAllByUsername(String username);
}
