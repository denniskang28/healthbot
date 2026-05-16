package com.healthbot.backend.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "service_providers")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ServiceProvider {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    // MEDICAL_LLM | ONLINE_CONSULTATION | OFFLINE_APPOINTMENT | ONLINE_PHARMACY | OTHER
    private String type;

    private String company;

    @Column(length = 2000)
    private String description;

    private boolean enabled = true;
    private int priority = 0;

    @Column(length = 2000)
    private String config; // JSON: endpoint, apiKey, etc.

    private LocalDateTime createdAt = LocalDateTime.now();
}
