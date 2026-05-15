package com.healthbot.backend.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthbot.backend.dto.*;
import com.healthbot.backend.model.*;
import com.healthbot.backend.repository.*;
import com.healthbot.backend.service.LlmProxyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
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
    private final ChatMessageRepository chatMessageRepository;
    private final LlmProxyService llmProxyService;
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

    @GetMapping("/users")
    public ResponseEntity<List<Map<String, Object>>> getAllUsers() {
        List<Map<String, Object>> result = userRepository.findAll().stream()
                .map(this::toUserDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/chat-history/{userId}")
    public ResponseEntity<List<ChatMessageDto>> getChatHistory(@PathVariable Long userId) {
        List<ChatMessage> msgs = chatMessageRepository.findByUserIdOrderByTimestampAsc(userId);
        List<ChatMessageDto> dtos = msgs.stream()
                .map(m -> new ChatMessageDto(m.getId(), m.getUserId(), m.getRole(), m.getContent(), m.getTimestamp()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @DeleteMapping("/chat-history/{userId}")
    @Transactional
    public ResponseEntity<Void> deleteChatHistory(@PathVariable Long userId) {
        chatMessageRepository.deleteByUserId(userId);
        llmProxyService.resetChatCounter(userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/llm-config")
    public ResponseEntity<Map<String, Object>> getLlmConfig() {
        Map<String, Object> cfg = llmProxyService.fetchConfig();
        if (cfg.isEmpty()) return ResponseEntity.status(503).build();
        return ResponseEntity.ok(cfg);
    }

    @PutMapping("/llm-config")
    public ResponseEntity<Map<String, Object>> updateLlmConfig(@RequestBody Map<String, Object> body) {
        Map<String, Object> result = llmProxyService.pushConfig(body);
        if (result.isEmpty()) return ResponseEntity.status(503).build();
        return ResponseEntity.ok(result);
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

}
