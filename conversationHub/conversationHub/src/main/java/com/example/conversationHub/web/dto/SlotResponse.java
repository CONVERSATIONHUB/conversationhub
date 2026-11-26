package com.example.conversationHub.web.dto;

import java.time.LocalDateTime;

public record SlotResponse(
        Long id,
        Long providerId,
        LocalDateTime startTime,
        LocalDateTime endTime,
        Integer capacity,
        Integer bookedCount,
        boolean blocked
) {
}
