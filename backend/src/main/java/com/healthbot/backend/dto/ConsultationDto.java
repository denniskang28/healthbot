package com.healthbot.backend.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConsultationDto {
    private Long id;
    private Long userId;
    private Long doctorId;
    private String type;
    private String status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String notes;
}
