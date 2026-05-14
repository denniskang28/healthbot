package com.healthbot.backend.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActionsDto {
    private boolean suggestConsultation;
    private String consultationType; // AI, DOCTOR, null
    private boolean suggestAppointment;
    private List<Long> recommendedDoctorIds;
}
