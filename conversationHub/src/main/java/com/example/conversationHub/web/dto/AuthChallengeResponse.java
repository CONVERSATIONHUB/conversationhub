package com.example.conversationHub.web.dto;

public record AuthChallengeResponse(
        Long challengeId,
        String code,
        String channel,
        String targetValue,
        Long userId
) {
}
