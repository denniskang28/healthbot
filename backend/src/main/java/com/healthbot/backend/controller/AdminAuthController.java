package com.healthbot.backend.controller;

import com.healthbot.backend.service.AdminAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/admin/auth")
@RequiredArgsConstructor
public class AdminAuthController {

    private final AdminAuthService authService;

    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@RequestBody Map<String, String> body) {
        String token = authService.login(body.get("password"));
        if (token == null) return ResponseEntity.status(401).body(Map.of("error", "Invalid password"));
        return ResponseEntity.ok(Map.of("token", token));
    }

    @PutMapping("/password")
    public ResponseEntity<Map<String, String>> changePassword(@RequestBody Map<String, String> body) {
        boolean ok = authService.changePassword(body.get("currentPassword"), body.get("newPassword"));
        if (!ok) return ResponseEntity.status(401).body(Map.of("error", "Current password incorrect"));
        return ResponseEntity.ok(Map.of("message", "Password updated. Please log in again."));
    }
}
