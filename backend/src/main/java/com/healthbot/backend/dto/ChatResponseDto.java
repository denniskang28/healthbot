package com.healthbot.backend.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponseDto {
    private ChatMessageDto userMessage;
    private ChatMessageDto assistantMessage;
    private ActionsDto actions;
}
