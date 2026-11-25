package com.example.conversationHub.web.dto;

import java.time.LocalDateTime;

public record NotificationView(
        Long id,
        String message,
        boolean read,
        LocalDateTime createdAt,
        Long bookingId
) {
}
