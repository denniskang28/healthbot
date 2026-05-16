package com.healthbot.backend.service;

import com.healthbot.backend.model.AdminCredential;
import com.healthbot.backend.repository.AdminCredentialRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminAuthService {

    private final AdminCredentialRepository repo;
    private volatile String activeToken = null;

    public String login(String password) {
        AdminCredential cred = repo.findAll().stream().findFirst().orElse(null);
        if (cred == null || !cred.getPasswordHash().equals(hash(password))) return null;
        activeToken = UUID.randomUUID().toString();
        return activeToken;
    }

    public boolean validateToken(String token) {
        return activeToken != null && activeToken.equals(token);
    }

    public boolean changePassword(String currentPassword, String newPassword) {
        AdminCredential cred = repo.findAll().stream().findFirst().orElse(null);
        if (cred == null || !cred.getPasswordHash().equals(hash(currentPassword))) return false;
        cred.setPasswordHash(hash(newPassword));
        repo.save(cred);
        activeToken = null; // invalidate existing sessions
        return true;
    }

    public String hash(String plain) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest(plain.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
