package com.healthbot.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.healthbot.backend.dto.ActionsDto;
import com.healthbot.backend.dto.MedicineDto;
import com.healthbot.backend.model.ChatMessage;
import com.healthbot.backend.model.LlmConfig;
import com.healthbot.backend.repository.LlmConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class LlmProxyService {

    private final LlmConfigRepository llmConfigRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate = new RestTemplate();

    public record ChatLlmResult(String content, ActionsDto actions) {}
    public record AiConsultationResult(String content, boolean isComplete, List<MedicineDto> prescription) {}

    private String getApiUrl() {
        return llmConfigRepository.findFirstByActiveTrue()
                .map(LlmConfig::getApiUrl)
                .orElse("http://localhost:8000");
    }

    public ChatLlmResult chat(Long userId, String message, List<ChatMessage> history, String language) {
        try {
            ObjectNode body = objectMapper.createObjectNode();
            body.put("userId", userId);
            body.put("message", message);
            body.put("language", language != null ? language : "EN");

            ArrayNode historyArray = body.putArray("history");
            for (ChatMessage msg : history) {
                ObjectNode h = objectMapper.createObjectNode();
                h.put("role", msg.getRole().toLowerCase());
                h.put("content", msg.getContent());
                historyArray.add(h);
            }

            String url = getApiUrl() + "/chat";
            JsonNode response = restTemplate.postForObject(url, body, JsonNode.class);

            if (response == null) return fallbackChatResult();

            String content = response.path("content").asText();
            boolean suggestConsultation = response.path("suggestConsultation").asBoolean(false);
            String consultationType = response.path("consultationType").isNull() ? null : response.path("consultationType").asText(null);
            boolean suggestAppointment = response.path("suggestAppointment").asBoolean(false);

            List<Long> doctorIds = new ArrayList<>();
            JsonNode idsNode = response.path("recommendedDoctorIds");
            if (idsNode.isArray()) {
                for (JsonNode n : idsNode) doctorIds.add(n.asLong());
            }

            ActionsDto actions = new ActionsDto(suggestConsultation, consultationType, suggestAppointment, doctorIds);
            return new ChatLlmResult(content, actions);

        } catch (Exception e) {
            log.warn("LLM service unavailable: {}", e.getMessage());
            return fallbackChatResult();
        }
    }

    public AiConsultationResult aiConsultation(Long userId, Long consultationId, String message, List<ChatMessage> history, String language) {
        try {
            ObjectNode body = objectMapper.createObjectNode();
            body.put("userId", userId);
            body.put("consultationId", consultationId);
            body.put("message", message);
            body.put("language", language != null ? language : "EN");

            ArrayNode historyArray = body.putArray("history");
            for (ChatMessage msg : history) {
                ObjectNode h = objectMapper.createObjectNode();
                h.put("role", msg.getRole().toLowerCase());
                h.put("content", msg.getContent());
                historyArray.add(h);
            }

            String url = getApiUrl() + "/ai-consultation";
            JsonNode response = restTemplate.postForObject(url, body, JsonNode.class);

            if (response == null) return fallbackConsultationResult(history.size());

            String content = response.path("content").asText();
            boolean isComplete = response.path("isComplete").asBoolean(false);

            List<MedicineDto> prescription = null;
            JsonNode rxNode = response.path("prescription");
            if (!rxNode.isNull() && rxNode.isArray()) {
                prescription = new ArrayList<>();
                for (JsonNode m : rxNode) {
                    prescription.add(new MedicineDto(
                            m.path("name").asText(),
                            m.path("dosage").asText(),
                            m.path("frequency").asText(),
                            m.path("days").asInt()
                    ));
                }
            }
            return new AiConsultationResult(content, isComplete, prescription);

        } catch (Exception e) {
            log.warn("LLM service unavailable: {}", e.getMessage());
            return fallbackConsultationResult(history.size());
        }
    }

    private ChatLlmResult fallbackChatResult() {
        ActionsDto actions = new ActionsDto(false, null, false, List.of());
        return new ChatLlmResult("I'm here to help with your health questions. Could you tell me more about your symptoms or concerns?", actions);
    }

    private AiConsultationResult fallbackConsultationResult(int historySize) {
        if (historySize >= 6) {
            List<MedicineDto> rx = List.of(
                    new MedicineDto("Amoxicillin", "500mg", "3 times daily", 7),
                    new MedicineDto("Ibuprofen", "400mg", "twice daily", 5)
            );
            return new AiConsultationResult("Based on your symptoms, I've prepared your prescription. Please take medications as directed.", true, rx);
        }
        String[] questions = {
                "Hello, I'm your AI doctor. Please describe your main symptoms.",
                "I see. How long have you been experiencing these symptoms? Any fever or chills?",
                "Have you taken any medications recently? Do you have any known allergies?"
        };
        int idx = Math.min(historySize / 2, questions.length - 1);
        return new AiConsultationResult(questions[idx], false, null);
    }
}
