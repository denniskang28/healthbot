package com.healthbot.backend.controller;

import com.healthbot.backend.dto.PurchaseDto;
import com.healthbot.backend.model.Purchase;
import com.healthbot.backend.repository.PurchaseRepository;
import com.healthbot.backend.service.SessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Random;

@RestController
@RequestMapping("/api/purchases")
@RequiredArgsConstructor
public class PurchaseController {

    private final PurchaseRepository purchaseRepository;
    private final SessionService sessionService;
    private final Random random = new Random();

    @PostMapping
    public ResponseEntity<PurchaseDto> createPurchase(@RequestBody Map<String, Object> body) {
        Long prescriptionId = Long.valueOf(body.get("prescriptionId").toString());
        Long userId = Long.valueOf(body.get("userId").toString());

        // Return existing purchase if already exists
        return purchaseRepository.findByPrescriptionId(prescriptionId)
                .map(p -> ResponseEntity.ok(toDto(p)))
                .orElseGet(() -> {
                    Purchase p = new Purchase();
                    p.setPrescriptionId(prescriptionId);
                    p.setUserId(userId);
                    p.setStatus("PENDING");
                    p.setTotalAmount(50 + random.nextDouble() * 250);
                    p.setCreatedAt(LocalDateTime.now());
                    purchaseRepository.save(p);
                    sessionService.updateState(userId, "PHARMACY");
                    return ResponseEntity.ok(toDto(p));
                });
    }

    @PutMapping("/{id}/complete")
    public ResponseEntity<PurchaseDto> completePurchase(@PathVariable Long id) {
        return purchaseRepository.findById(id).map(p -> {
            p.setStatus("COMPLETED");
            p.setPurchasedAt(LocalDateTime.now());
            purchaseRepository.save(p);
            sessionService.updateState(p.getUserId(), "IDLE");
            return ResponseEntity.ok(toDto(p));
        }).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PurchaseDto> getPurchase(@PathVariable Long id) {
        return purchaseRepository.findById(id)
                .map(p -> ResponseEntity.ok(toDto(p)))
                .orElse(ResponseEntity.notFound().build());
    }

    private PurchaseDto toDto(Purchase p) {
        return new PurchaseDto(p.getId(), p.getPrescriptionId(), p.getUserId(), p.getStatus(),
                p.getTotalAmount(), p.getPurchasedAt(), p.getCreatedAt());
    }
}
