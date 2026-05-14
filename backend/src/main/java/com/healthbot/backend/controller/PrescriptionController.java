package com.healthbot.backend.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthbot.backend.dto.MedicineDto;
import com.healthbot.backend.dto.PrescriptionDto;
import com.healthbot.backend.model.Prescription;
import com.healthbot.backend.repository.PrescriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/prescriptions")
@RequiredArgsConstructor
@Slf4j
public class PrescriptionController {

    private final PrescriptionRepository prescriptionRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostMapping
    public ResponseEntity<PrescriptionDto> createPrescription(@RequestBody Map<String, Object> body) {
        Long consultationId = Long.valueOf(body.get("consultationId").toString());
        Object medicinesObj = body.get("medicines");

        try {
            String medicinesJson = objectMapper.writeValueAsString(medicinesObj);
            Prescription p = new Prescription();
            p.setConsultationId(consultationId);
            p.setMedicinesJson(medicinesJson);
            p.setCreatedAt(LocalDateTime.now());
            prescriptionRepository.save(p);
            return ResponseEntity.ok(toDto(p));
        } catch (JsonProcessingException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<PrescriptionDto> getPrescription(@PathVariable Long id) {
        return prescriptionRepository.findById(id)
                .map(p -> ResponseEntity.ok(toDto(p)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/consultation/{consultationId}")
    public ResponseEntity<PrescriptionDto> getByConsultation(@PathVariable Long consultationId) {
        return prescriptionRepository.findByConsultationId(consultationId)
                .map(p -> ResponseEntity.ok(toDto(p)))
                .orElse(ResponseEntity.notFound().build());
    }

    PrescriptionDto toDto(Prescription p) {
        List<MedicineDto> medicines = List.of();
        try {
            medicines = objectMapper.readValue(p.getMedicinesJson(), new TypeReference<List<MedicineDto>>() {});
        } catch (Exception e) {
            log.warn("Failed to parse medicines JSON", e);
        }
        return new PrescriptionDto(p.getId(), p.getConsultationId(), medicines, p.getCreatedAt());
    }
}
