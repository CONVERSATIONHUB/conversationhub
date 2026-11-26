package com.example.conversationHub.web.dto;

import com.example.conversationHub.domain.SlotRecurrence;

import java.time.LocalDate;
import java.time.LocalTime;

public record RecurrenceView(
        Long id,
        Long providerId,
        Integer dayOfWeek,
        LocalTime startTime,
        LocalTime endTime,
        Integer capacity,
        LocalDate validFrom,
        LocalDate validUntil
) {

    public static RecurrenceView from(SlotRecurrence recurrence) {
        return new RecurrenceView(
                recurrence.getId(),
                recurrence.getProvider() == null ? null : recurrence.getProvider().getId(),
                recurrence.getDayOfWeek(),
                recurrence.getStartTime(),
                recurrence.getEndTime(),
                recurrence.getCapacity(),
                recurrence.getValidFrom(),
                recurrence.getValidUntil()
        );
    }
}
