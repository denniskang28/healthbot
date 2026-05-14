package com.healthbot.backend.repository;

import com.healthbot.backend.model.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    List<Appointment> findByUserIdOrderByCreatedAtDesc(Long userId);
    Optional<Appointment> findTopByUserIdOrderByCreatedAtDesc(Long userId);
}
