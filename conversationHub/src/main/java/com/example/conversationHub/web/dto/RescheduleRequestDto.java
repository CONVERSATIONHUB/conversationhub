package com.example.conversationHub.web.dto;

import com.example.conversationHub.domain.RescheduleRequest;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * Request payload for a reschedule and, with the response fields populated,
 * the shape returned by the "my reschedules" listing.
 */
public record RescheduleRequestDto(

        @NotNull(message = "New slot is required")
        Long newSlotId,

        @Size(max = 500, message = "Reason must not exceed 500 characters")
        String reason,

        Long id,
        Long bookingId,
        String status,
        LocalDateTime requestedAt,
        LocalDateTime decidedAt,
        String decisionReason,
        LocalDateTime newSlotStart,
        LocalDateTime newSlotEnd
) {

    public static RescheduleRequestDto request(Long newSlotId, String reason) {
        return new RescheduleRequestDto(newSlotId, reason, null, null, null, null, null,
                null, null, null);
    }

    public static RescheduleRequestDto from(RescheduleRequest reschedule) {
        return new RescheduleRequestDto(
                reschedule.getNewSlot() == null ? null : reschedule.getNewSlot().getId(),
                reschedule.getReason(),
                reschedule.getId(),
                reschedule.getBooking() == null ? null : reschedule.getBooking().getId(),
                reschedule.getStatus() == null ? null : reschedule.getStatus().name(),
                reschedule.getRequestedAt(),
                reschedule.getDecidedAt(),
                reschedule.getDecisionReason(),
                reschedule.getNewSlot() == null ? null : reschedule.getNewSlot().getStartTime(),
                reschedule.getNewSlot() == null ? null : reschedule.getNewSlot().getEndTime()
        );
    }
}
