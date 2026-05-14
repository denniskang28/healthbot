package com.healthbot.backend.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentDto {
    private Long id;
    private Long userId;
    private Long doctorId;
    private String doctorName;
    private LocalDateTime scheduledTime;
    private String status;
    private String hospitalName;
    private LocalDateTime createdAt;
}
