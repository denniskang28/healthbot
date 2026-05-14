package com.healthbot.backend.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "purchases")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Purchase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long prescriptionId;
    private Long userId;
    private String status = "PENDING"; // PENDING, COMPLETED
    private Double totalAmount;
    private LocalDateTime purchasedAt;
    private LocalDateTime createdAt = LocalDateTime.now();
}
