package com.healthbot.backend.repository;

import com.healthbot.backend.model.Consultation;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ConsultationRepository extends JpaRepository<Consultation, Long> {
    List<Consultation> findByUserIdOrderByStartTimeDesc(Long userId);
    Optional<Consultation> findTopByUserIdOrderByStartTimeDesc(Long userId);
}
