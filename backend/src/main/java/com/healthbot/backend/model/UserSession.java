package com.healthbot.backend.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_sessions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserSession {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private Long userId;

    private String currentState = "IDLE"; // IDLE, CHATTING, AI_CONSULTATION, DOCTOR_CONSULTATION, APPOINTMENT, PHARMACY

    private LocalDateTime lastActive = LocalDateTime.now();

    @Column(length = 2000)
    private String sessionData;
}
