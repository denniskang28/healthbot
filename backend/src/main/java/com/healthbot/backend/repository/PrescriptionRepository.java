package com.healthbot.backend.repository;

import com.healthbot.backend.model.Prescription;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PrescriptionRepository extends JpaRepository<Prescription, Long> {
    Optional<Prescription> findByConsultationId(Long consultationId);
}
