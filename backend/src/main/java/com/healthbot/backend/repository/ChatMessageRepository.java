package com.healthbot.backend.repository;

import com.healthbot.backend.model.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findByUserIdOrderByTimestampAsc(Long userId);
    List<ChatMessage> findTop20ByUserIdOrderByTimestampDesc(Long userId);
}
