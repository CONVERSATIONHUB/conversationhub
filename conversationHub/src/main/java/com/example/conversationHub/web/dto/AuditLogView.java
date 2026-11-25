package com.example.conversationHub.web.dto;

import java.time.LocalDateTime;

public record AuditLogView(
        Long id,
        Long userId,
        String action,
        String entityType,
        Long entityId,
        LocalDateTime performedAt
) {
}
