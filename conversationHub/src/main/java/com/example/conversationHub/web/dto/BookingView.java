package com.example.conversationHub.web.dto;

import com.example.conversationHub.domain.enums.BookingStatus;

import java.time.LocalDateTime;

public record BookingView(
        Long id,
        BookingStatus status,
        String providerName,
        String serviceType,
        LocalDateTime startTime,
        LocalDateTime endTime,
        String attendeeName,
        String relationship,
        LocalDateTime submittedAt,
        LocalDateTime expiresAt,
        LocalDateTime decidedAt,
        String decisionReason,
        String cancellationRule,
        Integer responseDeadlineHours
) {
}
