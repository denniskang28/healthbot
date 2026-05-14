package com.healthbot.backend.controller;

import com.healthbot.backend.dto.AppointmentDto;
import com.healthbot.backend.model.Appointment;
import com.healthbot.backend.model.Doctor;
import com.healthbot.backend.repository.AppointmentRepository;
import com.healthbot.backend.repository.DoctorRepository;
import com.healthbot.backend.service.SessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/appointments")
@RequiredArgsConstructor
public class AppointmentController {

    private final AppointmentRepository appointmentRepository;
    private final DoctorRepository doctorRepository;
    private final SessionService sessionService;

    @PostMapping
    public ResponseEntity<AppointmentDto> createAppointment(@RequestBody Map<String, Object> body) {
        Long userId = Long.valueOf(body.get("userId").toString());
        Long doctorId = Long.valueOf(body.get("doctorId").toString());
        String scheduledTimeStr = body.get("scheduledTime").toString();
        String hospitalName = body.get("hospitalName").toString();

        LocalDateTime scheduledTime = LocalDateTime.parse(scheduledTimeStr, DateTimeFormatter.ISO_DATE_TIME);

        Appointment a = new Appointment();
        a.setUserId(userId);
        a.setDoctorId(doctorId);
        a.setScheduledTime(scheduledTime);
        a.setHospitalName(hospitalName);
        a.setStatus("CONFIRMED");
        a.setCreatedAt(LocalDateTime.now());
        appointmentRepository.save(a);

        sessionService.updateState(userId, "APPOINTMENT");

        return ResponseEntity.ok(toDto(a));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AppointmentDto> getAppointment(@PathVariable Long id) {
        return appointmentRepository.findById(id)
                .map(a -> ResponseEntity.ok(toDto(a)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<AppointmentDto>> getUserAppointments(@PathVariable Long userId) {
        return ResponseEntity.ok(appointmentRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream().map(this::toDto).collect(Collectors.toList()));
    }

    @PutMapping("/{id}/confirm")
    public ResponseEntity<AppointmentDto> confirmAppointment(@PathVariable Long id) {
        return appointmentRepository.findById(id).map(a -> {
            a.setStatus("CONFIRMED");
            appointmentRepository.save(a);
            return ResponseEntity.ok(toDto(a));
        }).orElse(ResponseEntity.notFound().build());
    }

    private AppointmentDto toDto(Appointment a) {
        String doctorName = doctorRepository.findById(a.getDoctorId())
                .map(Doctor::getName).orElse("Unknown");
        return new AppointmentDto(a.getId(), a.getUserId(), a.getDoctorId(), doctorName,
                a.getScheduledTime(), a.getStatus(), a.getHospitalName(), a.getCreatedAt());
    }
}
