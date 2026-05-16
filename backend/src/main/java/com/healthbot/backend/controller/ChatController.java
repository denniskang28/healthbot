package com.healthbot.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthbot.backend.dto.*;
import com.healthbot.backend.model.ChatMessage;
import com.healthbot.backend.model.ServiceProvider;
import com.healthbot.backend.model.User;
import com.healthbot.backend.repository.ChatMessageRepository;
import com.healthbot.backend.repository.ConsultationRepository;
import com.healthbot.backend.repository.UserRepository;
import com.healthbot.backend.service.LlmProxyService;
import com.healthbot.backend.service.ProviderRoutingService;
import com.healthbot.backend.service.SessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
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
    private final ProviderRoutingService providerRoutingService;

    @PostMapping("/{userId}/message")
    public ResponseEntity<ChatResponseDto> sendMessage(
            @PathVariable Long userId,
            @RequestBody Map<String, String> body) {

        String content = body.get("content");
        User user = userRepository.findById(userId).orElse(null);
        String language = user != null ? user.getLanguage() : "EN";

        // Fetch history BEFORE saving current message — avoids duplication in LLM context
        List<ChatMessage> historyDesc = chatMessageRepository.findTop20ByUserIdOrderByTimestampDesc(userId);
        List<ChatMessage> history = new ArrayList<>(historyDesc.subList(0, Math.min(historyDesc.size(), 10)));
        Collections.reverse(history); // oldest-first so LLM sees chronological order

        // Save user message
        ChatMessage userMsg = new ChatMessage();
        userMsg.setUserId(userId);
        userMsg.setRole("USER");
        userMsg.setContent(content);
        userMsg.setTimestamp(LocalDateTime.now());
        chatMessageRepository.save(userMsg);

        // Update session state
        sessionService.updateState(userId, "CHATTING");

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

        ActionsDto actions = llmResult.actions();
        String recommendation = actions.getRecommendation();
        if (recommendation != null) {
            ServiceProvider provider = providerRoutingService.dispatch(recommendation, language, actions.getSpecialty(), userId);
            if (provider != null) {
                actions.setSelectedProviderId(provider.getId());
                actions.setSelectedProviderName(provider.getName());
                actions.setSelectedProviderCompany(provider.getCompany());
            }
        }

        return ResponseEntity.ok(new ChatResponseDto(userDto, assistantDto, actions));
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
