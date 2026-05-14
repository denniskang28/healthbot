package com.healthbot.backend.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "consultations")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Consultation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;
    private Long doctorId;
    private String type; // AI_CONSULTATION, DOCTOR_CONSULTATION
    private String status = "ACTIVE"; // ACTIVE, COMPLETED, CANCELLED

    private LocalDateTime startTime = LocalDateTime.now();
    private LocalDateTime endTime;

    @Column(length = 2000)
    private String notes;
}
