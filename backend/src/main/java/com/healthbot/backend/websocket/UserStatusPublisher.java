package com.healthbot.backend.websocket;

import com.healthbot.backend.dto.UserSessionDto;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserStatusPublisher {

    private final SimpMessagingTemplate messagingTemplate;

    public void publishUserStatus(UserSessionDto sessionDto) {
        messagingTemplate.convertAndSend("/topic/user-status", sessionDto);
    }
}
