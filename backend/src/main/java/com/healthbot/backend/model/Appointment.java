package com.healthbot.backend.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "appointments")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Appointment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;
    private Long doctorId;
    private LocalDateTime scheduledTime;
    private String status = "PENDING"; // PENDING, CONFIRMED, CANCELLED
    private String hospitalName;
    private LocalDateTime createdAt = LocalDateTime.now();
}
