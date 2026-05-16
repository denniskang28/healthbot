package com.healthbot.backend.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "routing_rules")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoutingRule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String serviceType; // same as ServiceProvider.type

    // JSON condition: {} | {"language":"ZH"} | {"language":"EN"}
    @Column(length = 500)
    private String conditionJson = "{}";

    private Long targetProviderId; // null = any enabled provider of that type

    private int priority = 0; // higher = evaluated first
    private boolean enabled = true;

    private LocalDateTime createdAt = LocalDateTime.now();
}
