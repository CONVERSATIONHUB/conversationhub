package com.example.conversationHub.web.dto;

public record AuthPasswordResetResponse(
        String token,
        Long tokenId,
        long expiresInSeconds
) {
}
