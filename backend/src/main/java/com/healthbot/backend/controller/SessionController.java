package com.healthbot.backend.controller;

import com.healthbot.backend.dto.UserSessionDto;
import com.healthbot.backend.service.SessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/sessions")
@RequiredArgsConstructor
public class SessionController {

    private final SessionService sessionService;

    @GetMapping("/{userId}")
    public ResponseEntity<UserSessionDto> getSession(@PathVariable Long userId) {
        return ResponseEntity.ok(sessionService.getSession(userId));
    }

    @PutMapping("/{userId}/state")
    public ResponseEntity<UserSessionDto> updateState(@PathVariable Long userId, @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(sessionService.updateState(userId, body.get("state")));
    }
}
