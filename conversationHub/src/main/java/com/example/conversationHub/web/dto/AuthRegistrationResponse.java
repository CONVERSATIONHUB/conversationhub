package com.example.conversationHub.web.dto;

public record AuthRegistrationResponse(
        Long userId,
        Long challengeId,
        String code,
        String channel
) {
}
