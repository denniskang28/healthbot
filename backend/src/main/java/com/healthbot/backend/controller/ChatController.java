package com.healthbot.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthbot.backend.dto.*;
import com.healthbot.backend.model.ChatMessage;
import com.healthbot.backend.model.User;
import com.healthbot.backend.repository.ChatMessageRepository;
import com.healthbot.backend.repository.ConsultationRepository;
import com.healthbot.backend.repository.UserRepository;
import com.healthbot.backend.service.LlmProxyService;
import com.healthbot.backend.service.SessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;
    private final LlmProxyService llmProxyService;
    private final SessionService sessionService;

    @PostMapping("/{userId}/message")
    public ResponseEntity<ChatResponseDto> sendMessage(
            @PathVariable Long userId,
            @RequestBody Map<String, String> body) {

        String content = body.get("content");
        User user = userRepository.findById(userId).orElse(null);
        String language = user != null ? user.getLanguage() : "EN";

        // Save user message
        ChatMessage userMsg = new ChatMessage();
        userMsg.setUserId(userId);
        userMsg.setRole("USER");
        userMsg.setContent(content);
        userMsg.setTimestamp(LocalDateTime.now());
        chatMessageRepository.save(userMsg);

        // Update session state
        sessionService.updateState(userId, "CHATTING");

        // Get history
        List<ChatMessage> history = chatMessageRepository.findTop20ByUserIdOrderByTimestampDesc(userId);
        history = history.subList(0, Math.min(history.size(), 10));

        // Call LLM
        LlmProxyService.ChatLlmResult llmResult = llmProxyService.chat(userId, content, history, language);

        // Save assistant message
        ChatMessage assistantMsg = new ChatMessage();
        assistantMsg.setUserId(userId);
        assistantMsg.setRole("ASSISTANT");
        assistantMsg.setContent(llmResult.content());
        assistantMsg.setTimestamp(LocalDateTime.now());
        chatMessageRepository.save(assistantMsg);

        ChatMessageDto userDto = toDto(userMsg);
        ChatMessageDto assistantDto = toDto(assistantMsg);

        return ResponseEntity.ok(new ChatResponseDto(userDto, assistantDto, llmResult.actions()));
    }

    @GetMapping("/{userId}/history")
    public ResponseEntity<List<ChatMessageDto>> getHistory(@PathVariable Long userId) {
        List<ChatMessage> messages = chatMessageRepository.findByUserIdOrderByTimestampAsc(userId);
        return ResponseEntity.ok(messages.stream().map(this::toDto).collect(Collectors.toList()));
    }

    private ChatMessageDto toDto(ChatMessage msg) {
        return new ChatMessageDto(msg.getId(), msg.getUserId(), msg.getRole(), msg.getContent(), msg.getTimestamp());
    }
}
