package com.healthbot.backend.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserSessionDto {
    private Long id;
    private Long userId;
    private String userName;
    private String currentState;
    private LocalDateTime lastActive;
    private String sessionData;
}
