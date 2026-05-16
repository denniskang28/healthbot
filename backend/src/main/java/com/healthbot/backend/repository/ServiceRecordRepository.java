package com.healthbot.backend.repository;

import com.healthbot.backend.model.ServiceRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface ServiceRecordRepository extends JpaRepository<ServiceRecord, Long> {
    List<ServiceRecord> findByProviderIdOrderByCreatedAtDesc(Long providerId);
    long countByProviderId(Long providerId);

    @Query("SELECT AVG(r.rating) FROM ServiceRecord r WHERE r.providerId = :providerId AND r.rating IS NOT NULL")
    Double avgRatingByProviderId(Long providerId);
}
