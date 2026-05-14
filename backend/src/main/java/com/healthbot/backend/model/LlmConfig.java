package com.healthbot.backend.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "llm_configs")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LlmConfig {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String provider;
    private String model;
    private String apiUrl;

    @Column(length = 500)
    private String apiKey;

    @Column(length = 4000)
    private String systemPrompt;

    private Boolean active = true;
}
