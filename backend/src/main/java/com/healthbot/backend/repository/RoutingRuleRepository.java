package com.healthbot.backend.repository;

import com.healthbot.backend.model.RoutingRule;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface RoutingRuleRepository extends JpaRepository<RoutingRule, Long> {
    List<RoutingRule> findByServiceTypeAndEnabledTrueOrderByPriorityDesc(String serviceType);
    List<RoutingRule> findAllByOrderByServiceTypeAscPriorityDesc();
}
