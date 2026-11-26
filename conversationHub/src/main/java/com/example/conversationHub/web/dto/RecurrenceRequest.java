package com.example.conversationHub.web.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * dayOfWeek uses java.time.DayOfWeek numbering: Monday = 1 ... Sunday = 7.
 */
public record RecurrenceRequest(

        @NotNull(message = "Day of week is required")
        @Min(value = 1, message = "Day of week must be between 1 and 7")
        @Max(value = 7, message = "Day of week must be between 1 and 7")
        Integer dayOfWeek,

        @NotNull(message = "Start time is required")
        LocalTime startTime,

        @NotNull(message = "End time is required")
        LocalTime endTime,

        @NotNull(message = "Capacity is required")
        @Min(value = 1, message = "Capacity must be at least 1")
        Integer capacity,

        @NotNull(message = "Valid from date is required")
        LocalDate validFrom,

        @NotNull(message = "Valid until date is required")
        LocalDate validUntil
) {

    @AssertTrue(message = "End time must be after start time")
    public boolean isTimeRangeValid() {
        return startTime != null && endTime != null && endTime.isAfter(startTime);
    }

    @AssertTrue(message = "Valid until must not be before valid from")
    public boolean isDateRangeValid() {
        return validFrom != null && validUntil != null && !validUntil.isBefore(validFrom);
    }
}
