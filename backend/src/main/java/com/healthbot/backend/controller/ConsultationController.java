package com.healthbot.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthbot.backend.dto.*;
import com.healthbot.backend.model.*;
import com.healthbot.backend.repository.*;
import com.healthbot.backend.service.LlmProxyService;
import com.healthbot.backend.service.SessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class ConsultationController {

    private final ConsultationRepository consultationRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;
    private final LlmProxyService llmProxyService;
    private final SessionService sessionService;

    @PostMapping("/api/consultations")
    public ResponseEntity<ConsultationDto> createConsultation(@RequestBody Map<String, Object> body) {
        Long userId = Long.valueOf(body.get("userId").toString());
        Long doctorId = body.get("doctorId") != null ? Long.valueOf(body.get("doctorId").toString()) : null;
        String type = body.get("type").toString();

        Consultation c = new Consultation();
        c.setUserId(userId);
        c.setDoctorId(doctorId);
        c.setType(type);
        c.setStatus("ACTIVE");
        c.setStartTime(LocalDateTime.now());
        consultationRepository.save(c);

        String state = "AI_CONSULTATION".equals(type) ? "AI_CONSULTATION" : "DOCTOR_CONSULTATION";
        sessionService.updateState(userId, state);

        return ResponseEntity.ok(toDto(c));
    }

    @GetMapping("/api/consultations/{id}")
    public ResponseEntity<ConsultationDto> getConsultation(@PathVariable Long id) {
        return consultationRepository.findById(id)
                .map(c -> ResponseEntity.ok(toDto(c)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/api/consultations/{id}/complete")
    public ResponseEntity<ConsultationDto> completeConsultation(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return consultationRepository.findById(id).map(c -> {
            c.setStatus("COMPLETED");
            c.setEndTime(LocalDateTime.now());
            c.setNotes(body.getOrDefault("notes", ""));
            consultationRepository.save(c);
            sessionService.updateState(c.getUserId(), "PHARMACY");
            return ResponseEntity.ok(toDto(c));
        }).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/api/ai-consultation/{userId}/message")
    public ResponseEntity<Map<String, Object>> sendAiMessage(
            @PathVariable Long userId,
            @RequestBody Map<String, Object> body) {

        Long consultationId = Long.valueOf(body.get("consultationId").toString());
        String content = body.get("content").toString();

        User user = userRepository.findById(userId).orElse(null);
        String language = user != null ? user.getLanguage() : "EN";

        ChatMessage userMsg = new ChatMessage();
        userMsg.setUserId(userId);
        userMsg.setRole("USER");
        userMsg.setContent(content);
        userMsg.setTimestamp(LocalDateTime.now());
        chatMessageRepository.save(userMsg);

        List<ChatMessage> history = chatMessageRepository.findTop20ByUserIdOrderByTimestampDesc(userId);

        LlmProxyService.AiConsultationResult result = llmProxyService.aiConsultation(userId, consultationId, content, history, language);

        ChatMessage assistantMsg = new ChatMessage();
        assistantMsg.setUserId(userId);
        assistantMsg.setRole("ASSISTANT");
        assistantMsg.setContent(result.content());
        assistantMsg.setTimestamp(LocalDateTime.now());
        chatMessageRepository.save(assistantMsg);

        ChatMessageDto msgDto = new ChatMessageDto(assistantMsg.getId(), userId, "ASSISTANT", result.content(), assistantMsg.getTimestamp());

        Map<String, Object> response = new java.util.HashMap<>();
        response.put("message", msgDto);
        response.put("isComplete", result.isComplete());
        response.put("prescription", result.prescription());
        return ResponseEntity.ok(response);
    }

    private ConsultationDto toDto(Consultation c) {
        return new ConsultationDto(c.getId(), c.getUserId(), c.getDoctorId(), c.getType(), c.getStatus(), c.getStartTime(), c.getEndTime(), c.getNotes());
    }
}
