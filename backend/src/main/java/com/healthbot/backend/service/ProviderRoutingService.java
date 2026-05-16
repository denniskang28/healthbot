package com.healthbot.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthbot.backend.model.RoutingRule;
import com.healthbot.backend.model.ServiceProvider;
import com.healthbot.backend.model.ServiceRecord;
import com.healthbot.backend.repository.RoutingRuleRepository;
import com.healthbot.backend.repository.ServiceProviderRepository;
import com.healthbot.backend.repository.ServiceRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProviderRoutingService {

    private final ServiceProviderRepository providerRepo;
    private final ServiceRecordRepository recordRepo;
    private final RoutingRuleRepository ruleRepo;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // recommendation type from LLM → provider type
    private static final Map<String, String> TYPE_MAP = Map.of(
            "ONLINE_CONSULTATION", "ONLINE_CONSULTATION",
            "OFFLINE_APPOINTMENT", "OFFLINE_APPOINTMENT",
            "MEDICATION",          "ONLINE_PHARMACY"
    );

    /**
     * Select provider, record the dispatch, and return the provider (or null if none configured).
     */
    public ServiceProvider selectMedicalLlm(String language, String specialty) {
        return selectProvider("MEDICAL_LLM", language, specialty);
    }

    public ServiceProvider dispatch(String recommendation, String language, String specialty, Long userId) {
        String providerType = TYPE_MAP.get(recommendation);
        if (providerType == null) return null;

        ServiceProvider selected = selectProvider(providerType, language, specialty);
        if (selected == null) return null;

        ServiceRecord record = new ServiceRecord();
        record.setProviderId(selected.getId());
        record.setUserId(userId);
        record.setServiceType(providerType);
        record.setStatus("DISPATCHED");
        recordRepo.save(record);

        return selected;
    }

    private ServiceProvider selectProvider(String type, String language, String specialty) {
        List<RoutingRule> rules = ruleRepo.findByServiceTypeAndEnabledTrueOrderByPriorityDesc(type);

        for (RoutingRule rule : rules) {
            if (!conditionMatches(rule.getConditionJson(), language, specialty)) continue;

            if (rule.getTargetProviderId() != null) {
                ServiceProvider target = providerRepo.findById(rule.getTargetProviderId()).orElse(null);
                if (target != null && target.isEnabled()) return target;
            } else {
                // rule matches but no specific target → fallthrough to default
                break;
            }
        }

        // Default: highest-priority enabled provider of this type
        List<ServiceProvider> providers = providerRepo.findByTypeAndEnabledTrueOrderByPriorityDesc(type);
        return providers.isEmpty() ? null : providers.get(0);
    }

    // All conditions in the JSON must match (AND logic).
    // Supported keys: "language" (EN/ZH), "specialty" (CARDIOLOGY, NEUROLOGY, etc.)
    private boolean conditionMatches(String conditionJson, String language, String specialty) {
        try {
            JsonNode node = objectMapper.readTree(conditionJson);
            if (node.has("language")) {
                String lang = language != null ? language : "EN";
                if (!node.get("language").asText().equalsIgnoreCase(lang)) return false;
            }
            if (node.has("specialty")) {
                if (specialty == null) return false;
                if (!node.get("specialty").asText().equalsIgnoreCase(specialty)) return false;
            }
            return true;
        } catch (Exception e) {
            return true;
        }
    }
}
