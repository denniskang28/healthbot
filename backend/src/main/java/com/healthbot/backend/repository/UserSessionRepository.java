package com.healthbot.backend.repository;

import com.healthbot.backend.model.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserSessionRepository extends JpaRepository<UserSession, Long> {
    Optional<UserSession> findByUserId(Long userId);
}
