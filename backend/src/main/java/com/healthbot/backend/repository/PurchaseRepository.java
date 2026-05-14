package com.healthbot.backend.repository;

import com.healthbot.backend.model.Purchase;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface PurchaseRepository extends JpaRepository<Purchase, Long> {
    List<Purchase> findByUserIdOrderByCreatedAtDesc(Long userId);
    Optional<Purchase> findTopByUserIdOrderByCreatedAtDesc(Long userId);
    Optional<Purchase> findByPrescriptionId(Long prescriptionId);
}
