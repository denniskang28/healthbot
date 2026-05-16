package com.healthbot.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActionsDto {
    private boolean suggestConsultation;
    private String consultationType;
    private boolean suggestAppointment;
    private List<Long> recommendedDoctorIds;
    // @JsonProperty forces Jackson to use "isComplete" instead of "IsComplete"
    // (Lombok generates isIsComplete() getter which Jackson would map to "IsComplete")
    @JsonProperty("isComplete")
    private boolean isComplete;
    private String conclusion;
    private String recommendation; // ONLINE_CONSULTATION | OFFLINE_APPOINTMENT | MEDICATION
    private String specialty;      // CARDIOLOGY | NEUROLOGY | DERMATOLOGY | ORTHOPEDICS | GASTROENTEROLOGY | RESPIRATORY | ENDOCRINOLOGY | PSYCHIATRY | PEDIATRICS | GENERAL
    private List<MedicineDto> prescription;
    private Long selectedProviderId;
    private String selectedProviderName;
    private String selectedProviderCompany;
}
