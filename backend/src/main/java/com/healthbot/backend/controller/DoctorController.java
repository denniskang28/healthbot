package com.healthbot.backend.controller;

import com.healthbot.backend.dto.DoctorDto;
import com.healthbot.backend.model.Doctor;
import com.healthbot.backend.repository.DoctorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/doctors")
@RequiredArgsConstructor
public class DoctorController {

    private final DoctorRepository doctorRepository;

    @GetMapping
    public ResponseEntity<List<DoctorDto>> getDoctors() {
        return ResponseEntity.ok(doctorRepository.findAll().stream().map(this::toDto).collect(Collectors.toList()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DoctorDto> getDoctor(@PathVariable Long id) {
        return doctorRepository.findById(id)
                .map(d -> ResponseEntity.ok(toDto(d)))
                .orElse(ResponseEntity.notFound().build());
    }

    private DoctorDto toDto(Doctor d) {
        return new DoctorDto(d.getId(), d.getName(), d.getSpecialty(), d.getBio(), d.getRating(), d.getAvailable(), d.getAvatarInitials());
    }
}
