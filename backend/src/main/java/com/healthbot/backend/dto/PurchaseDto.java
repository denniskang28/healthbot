package com.healthbot.backend.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseDto {
    private Long id;
    private Long prescriptionId;
    private Long userId;
    private String status;
    private Double totalAmount;
    private LocalDateTime purchasedAt;
    private LocalDateTime createdAt;
}
