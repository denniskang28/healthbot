package com.healthbot.backend.service;

import com.healthbot.backend.dto.UserSessionDto;
import com.healthbot.backend.model.User;
import com.healthbot.backend.model.UserSession;
import com.healthbot.backend.repository.UserRepository;
import com.healthbot.backend.repository.UserSessionRepository;
import com.healthbot.backend.websocket.UserStatusPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class SessionService {

    private final UserSessionRepository userSessionRepository;
    private final UserRepository userRepository;
    private final UserStatusPublisher userStatusPublisher;

    public UserSessionDto getSession(Long userId) {
        UserSession session = userSessionRepository.findByUserId(userId)
                .orElseGet(() -> createSession(userId));
        return toDto(session);
    }

    public UserSessionDto updateState(Long userId, String state) {
        UserSession session = userSessionRepository.findByUserId(userId)
                .orElseGet(() -> createSession(userId));
        session.setCurrentState(state);
        session.setLastActive(LocalDateTime.now());
        userSessionRepository.save(session);
        UserSessionDto dto = toDto(session);
        userStatusPublisher.publishUserStatus(dto);
        return dto;
    }

    private UserSession createSession(Long userId) {
        UserSession session = new UserSession();
        session.setUserId(userId);
        session.setCurrentState("IDLE");
        return userSessionRepository.save(session);
    }

    private UserSessionDto toDto(UserSession session) {
        String userName = userRepository.findById(session.getUserId())
                .map(User::getName).orElse("Unknown");
        return new UserSessionDto(
                session.getId(), session.getUserId(), userName,
                session.getCurrentState(), session.getLastActive(), session.getSessionData()
        );
    }
}
