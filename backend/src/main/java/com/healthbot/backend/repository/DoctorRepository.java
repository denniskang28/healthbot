package com.healthbot.backend.repository;

import com.healthbot.backend.model.Doctor;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DoctorRepository extends JpaRepository<Doctor, Long> {
    List<Doctor> findByAvailableTrue();
}
