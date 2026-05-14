package com.healthbot.backend.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthbot.backend.dto.*;
import com.healthbot.backend.model.*;
import com.healthbot.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserRepository userRepository;
    private final UserSessionRepository userSessionRepository;
    private final ConsultationRepository consultationRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final PurchaseRepository purchaseRepository;
    private final DoctorRepository doctorRepository;
    private final LlmConfigRepository llmConfigRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @GetMapping("/users/active")
    public ResponseEntity<List<Map<String, Object>>> getActiveUsers() {
        List<Map<String, Object>> result = new ArrayList<>();

        List<UserSession> sessions = userSessionRepository.findAll();
        for (UserSession session : sessions) {
            userRepository.findById(session.getUserId()).ifPresent(user -> {
                Map<String, Object> entry = new HashMap<>();
                entry.put("user", toUserDto(user));

                UserSessionDto sessionDto = new UserSessionDto(
                        session.getId(), session.getUserId(), user.getName(),
                        session.getCurrentState(), session.getLastActive(), session.getSessionData());
                entry.put("session", sessionDto);

                consultationRepository.findTopByUserIdOrderByStartTimeDesc(session.getUserId())
                        .ifPresent(c -> entry.put("latestConsultation", toConsultationDto(c)));

                consultationRepository.findTopByUserIdOrderByStartTimeDesc(session.getUserId())
                        .flatMap(c -> prescriptionRepository.findByConsultationId(c.getId()))
                        .ifPresent(p -> entry.put("latestPrescription", toPrescriptionDto(p)));

                purchaseRepository.findTopByUserIdOrderByCreatedAtDesc(session.getUserId())
                        .ifPresent(p -> entry.put("latestPurchase", toPurchaseDto(p)));

                result.add(entry);
            });
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/consultations")
    public ResponseEntity<List<Map<String, Object>>> getConsultations() {
        List<Map<String, Object>> result = new ArrayList<>();
        List<Consultation> consultations = consultationRepository.findAll();

        for (Consultation c : consultations) {
            Map<String, Object> entry = new HashMap<>();
            entry.put("consultation", toConsultationDto(c));
            userRepository.findById(c.getUserId()).ifPresent(u -> entry.put("user", toUserDto(u)));
            if (c.getDoctorId() != null) {
                doctorRepository.findById(c.getDoctorId()).ifPresent(d -> entry.put("doctor", toDoctorDto(d)));
            }
            prescriptionRepository.findByConsultationId(c.getId())
                    .ifPresent(p -> entry.put("prescription", toPrescriptionDto(p)));
            result.add(entry);
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/purchases")
    public ResponseEntity<List<Map<String, Object>>> getPurchases() {
        List<Map<String, Object>> result = new ArrayList<>();
        List<Purchase> purchases = purchaseRepository.findAll();

        for (Purchase p : purchases) {
            Map<String, Object> entry = new HashMap<>();
            entry.put("purchase", toPurchaseDto(p));
            userRepository.findById(p.getUserId()).ifPresent(u -> entry.put("user", toUserDto(u)));
            prescriptionRepository.findById(p.getPrescriptionId())
                    .ifPresent(rx -> entry.put("prescription", toPrescriptionDto(rx)));
            result.add(entry);
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/llm-config")
    public ResponseEntity<LlmConfigDto> getLlmConfig() {
        return llmConfigRepository.findFirstByActiveTrue()
                .map(this::toLlmConfigDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/llm-config")
    public ResponseEntity<LlmConfigDto> updateLlmConfig(@RequestBody Map<String, Object> body) {
        LlmConfig config = llmConfigRepository.findFirstByActiveTrue()
                .orElse(new LlmConfig());

        if (body.containsKey("provider")) config.setProvider(body.get("provider").toString());
        if (body.containsKey("model")) config.setModel(body.get("model").toString());
        if (body.containsKey("apiUrl")) config.setApiUrl(body.get("apiUrl").toString());
        String newKey = body.containsKey("apiKey") ? body.get("apiKey").toString() : "";
        if (!newKey.isBlank()) config.setApiKey(newKey);
        if (body.containsKey("systemPrompt")) config.setSystemPrompt(body.get("systemPrompt").toString());
        if (body.containsKey("active")) config.setActive(Boolean.valueOf(body.get("active").toString()));

        llmConfigRepository.save(config);
        return ResponseEntity.ok(toLlmConfigDto(config));
    }

    private Map<String, Object> toUserDto(User u) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", u.getId()); m.put("name", u.getName()); m.put("phone", u.getPhone()); m.put("language", u.getLanguage());
        return m;
    }

    private ConsultationDto toConsultationDto(Consultation c) {
        return new ConsultationDto(c.getId(), c.getUserId(), c.getDoctorId(), c.getType(), c.getStatus(), c.getStartTime(), c.getEndTime(), c.getNotes());
    }

    private Map<String, Object> toDoctorDto(Doctor d) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", d.getId()); m.put("name", d.getName()); m.put("specialty", d.getSpecialty());
        return m;
    }

    private PrescriptionDto toPrescriptionDto(Prescription p) {
        List<MedicineDto> medicines = List.of();
        try {
            medicines = objectMapper.readValue(p.getMedicinesJson(), new TypeReference<List<MedicineDto>>() {});
        } catch (Exception ignored) {}
        return new PrescriptionDto(p.getId(), p.getConsultationId(), medicines, p.getCreatedAt());
    }

    private PurchaseDto toPurchaseDto(Purchase p) {
        return new PurchaseDto(p.getId(), p.getPrescriptionId(), p.getUserId(), p.getStatus(),
                p.getTotalAmount(), p.getPurchasedAt(), p.getCreatedAt());
    }

    private LlmConfigDto toLlmConfigDto(LlmConfig c) {
        String masked = "";
        if (c.getApiKey() != null && c.getApiKey().length() > 4) {
            masked = "****..." + c.getApiKey().substring(c.getApiKey().length() - 4);
        } else if (c.getApiKey() != null && !c.getApiKey().isEmpty()) {
            masked = "****";
        }
        return new LlmConfigDto(c.getId(), c.getProvider(), c.getModel(), c.getApiUrl(), masked, c.getSystemPrompt(), c.getActive());
    }
}
