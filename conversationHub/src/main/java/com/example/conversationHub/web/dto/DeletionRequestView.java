package com.example.conversationHub.web.dto;

import java.time.LocalDateTime;

public record DeletionRequestView(
        Long id,
        Long userId,
        String status,
        String reason,
        LocalDateTime requestedAt,
        LocalDateTime resolvedAt
) {
}
