package com.example.conversationHub.web.dto;

import java.time.LocalDateTime;

public record BookingReportRow(
        Long id,
        String status,
        String institutionName,
        String providerName,
        String serviceCategory,
        String serviceType,
        String seekerName,
        String seekerPhone,
        String seekerEmail,
        boolean bookingForOther,
        String attendeeName,
        String relationship,
        LocalDateTime startTime,
        LocalDateTime endTime,
        LocalDateTime submittedAt,
        LocalDateTime decidedAt,
        String cancellationRule,
        String message
) {
}
