package com.healthbot.backend.repository;

import com.healthbot.backend.model.LlmConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface LlmConfigRepository extends JpaRepository<LlmConfig, Long> {
    Optional<LlmConfig> findFirstByActiveTrue();
}
