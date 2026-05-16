package com.healthbot.backend.controller;

import com.healthbot.backend.model.RoutingRule;
import com.healthbot.backend.model.ServiceProvider;
import com.healthbot.backend.model.ServiceRecord;
import com.healthbot.backend.repository.RoutingRuleRepository;
import com.healthbot.backend.repository.ServiceProviderRepository;
import com.healthbot.backend.repository.ServiceRecordRepository;
import com.healthbot.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class ProviderController {

    private final ServiceProviderRepository providerRepo;
    private final ServiceRecordRepository recordRepo;
    private final RoutingRuleRepository ruleRepo;
    private final UserRepository userRepo;

    // ── Providers ────────────────────────────────────────────────────────────

    @GetMapping("/providers/{id}")
    public ResponseEntity<Map<String, Object>> getProvider(@PathVariable Long id) {
        return providerRepo.findById(id)
                .map(p -> ResponseEntity.ok(toProviderMap(p)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/providers")
    public ResponseEntity<List<Map<String, Object>>> listProviders() {
        return ResponseEntity.ok(
                providerRepo.findAll().stream()
                        .sorted(Comparator.comparing(ServiceProvider::getType)
                                .thenComparing(Comparator.comparingInt(ServiceProvider::getPriority).reversed()))
                        .map(this::toProviderMap)
                        .collect(Collectors.toList())
        );
    }

    @PostMapping("/providers")
    public ResponseEntity<Map<String, Object>> createProvider(@RequestBody ServiceProvider body) {
        body.setId(null);
        body.setCreatedAt(LocalDateTime.now());
        return ResponseEntity.ok(toProviderMap(providerRepo.save(body)));
    }

    @PutMapping("/providers/{id}")
    public ResponseEntity<Map<String, Object>> updateProvider(@PathVariable Long id, @RequestBody ServiceProvider body) {
        return providerRepo.findById(id).map(p -> {
            p.setName(body.getName());
            p.setType(body.getType());
            p.setCompany(body.getCompany());
            p.setDescription(body.getDescription());
            p.setEnabled(body.isEnabled());
            p.setPriority(body.getPriority());
            p.setConfig(body.getConfig());
            return ResponseEntity.ok(toProviderMap(providerRepo.save(p)));
        }).orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/providers/{id}/toggle")
    public ResponseEntity<Map<String, Object>> toggleProvider(@PathVariable Long id) {
        return providerRepo.findById(id).map(p -> {
            p.setEnabled(!p.isEnabled());
            return ResponseEntity.ok(toProviderMap(providerRepo.save(p)));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/providers/{id}")
    @Transactional
    public ResponseEntity<Void> deleteProvider(@PathVariable Long id) {
        providerRepo.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/providers/{id}/records")
    public ResponseEntity<List<Map<String, Object>>> getProviderRecords(@PathVariable Long id) {
        List<ServiceRecord> records = recordRepo.findByProviderIdOrderByCreatedAtDesc(id);
        List<Map<String, Object>> result = records.stream().map(r -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", r.getId());
            m.put("userId", r.getUserId());
            String userName = userRepo.findById(r.getUserId())
                    .map(u -> u.getName()).orElse("User " + r.getUserId());
            m.put("userName", userName);
            m.put("serviceType", r.getServiceType());
            m.put("status", r.getStatus());
            m.put("rating", r.getRating());
            m.put("notes", r.getNotes());
            m.put("createdAt", r.getCreatedAt());
            return m;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    @PatchMapping("/providers/records/{recordId}/rate")
    public ResponseEntity<Void> rateRecord(@PathVariable Long recordId, @RequestBody Map<String, Object> body) {
        recordRepo.findById(recordId).ifPresent(r -> {
            r.setRating(((Number) body.get("rating")).intValue());
            r.setStatus("COMPLETED");
            r.setUpdatedAt(LocalDateTime.now());
            if (body.containsKey("notes")) r.setNotes((String) body.get("notes"));
            recordRepo.save(r);
        });
        return ResponseEntity.ok().build();
    }

    // ── Routing Rules ────────────────────────────────────────────────────────

    @GetMapping("/routing-rules")
    public ResponseEntity<List<Map<String, Object>>> listRules() {
        return ResponseEntity.ok(
                ruleRepo.findAllByOrderByServiceTypeAscPriorityDesc().stream()
                        .map(this::toRuleMap)
                        .collect(Collectors.toList())
        );
    }

    @PostMapping("/routing-rules")
    public ResponseEntity<Map<String, Object>> createRule(@RequestBody RoutingRule body) {
        body.setId(null);
        body.setCreatedAt(LocalDateTime.now());
        return ResponseEntity.ok(toRuleMap(ruleRepo.save(body)));
    }

    @PutMapping("/routing-rules/{id}")
    public ResponseEntity<Map<String, Object>> updateRule(@PathVariable Long id, @RequestBody RoutingRule body) {
        return ruleRepo.findById(id).map(r -> {
            r.setName(body.getName());
            r.setServiceType(body.getServiceType());
            r.setConditionJson(body.getConditionJson());
            r.setTargetProviderId(body.getTargetProviderId());
            r.setPriority(body.getPriority());
            r.setEnabled(body.isEnabled());
            return ResponseEntity.ok(toRuleMap(ruleRepo.save(r)));
        }).orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/routing-rules/{id}/toggle")
    public ResponseEntity<Map<String, Object>> toggleRule(@PathVariable Long id) {
        return ruleRepo.findById(id).map(r -> {
            r.setEnabled(!r.isEnabled());
            return ResponseEntity.ok(toRuleMap(ruleRepo.save(r)));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/routing-rules/{id}")
    public ResponseEntity<Void> deleteRule(@PathVariable Long id) {
        ruleRepo.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private Map<String, Object> toProviderMap(ServiceProvider p) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", p.getId());
        m.put("name", p.getName());
        m.put("type", p.getType());
        m.put("company", p.getCompany());
        m.put("description", p.getDescription());
        m.put("enabled", p.isEnabled());
        m.put("priority", p.getPriority());
        m.put("config", p.getConfig());
        m.put("createdAt", p.getCreatedAt());
        m.put("serviceCount", recordRepo.countByProviderId(p.getId()));
        m.put("avgRating", recordRepo.avgRatingByProviderId(p.getId()));
        return m;
    }

    private Map<String, Object> toRuleMap(RoutingRule r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", r.getId());
        m.put("name", r.getName());
        m.put("serviceType", r.getServiceType());
        m.put("conditionJson", r.getConditionJson());
        m.put("targetProviderId", r.getTargetProviderId());
        m.put("priority", r.getPriority());
        m.put("enabled", r.isEnabled());
        m.put("createdAt", r.getCreatedAt());
        if (r.getTargetProviderId() != null) {
            providerRepo.findById(r.getTargetProviderId())
                    .ifPresent(p -> m.put("targetProviderName", p.getName()));
        }
        return m;
    }
}
