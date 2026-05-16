package com.healthbot.backend.repository;

import com.healthbot.backend.model.ServiceProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ServiceProviderRepository extends JpaRepository<ServiceProvider, Long> {
    List<ServiceProvider> findByTypeAndEnabledTrueOrderByPriorityDesc(String type);
    List<ServiceProvider> findByTypeOrderByPriorityDesc(String type);
}
