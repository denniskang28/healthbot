package com.healthbot.backend.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LlmConfigDto {
    private Long id;
    private String provider;
    private String model;
    private String apiUrl;
    private String apiKeyMasked; // e.g. "****...xxxx"
    private String systemPrompt;
    private Boolean active;
}
