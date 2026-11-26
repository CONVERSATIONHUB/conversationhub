package com.example.conversationHub.web.dto;

import java.time.LocalDateTime;

public record SlotView(
        Long id,
        LocalDateTime startTime,
        LocalDateTime endTime,
        Integer capacity,
        Integer remainingCapacity
) {
}
