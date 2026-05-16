package com.healthbot.backend.repository;

import com.healthbot.backend.model.AdminCredential;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminCredentialRepository extends JpaRepository<AdminCredential, Long> {}
