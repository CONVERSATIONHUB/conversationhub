package com.example.conversationHub.web.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateBookingRequest(

        @NotNull(message = "Provider is required")
        Long providerId,

        @NotNull(message = "Slot is required")
        Long slotId,

        @Size(max = 1000, message = "Message must not exceed 1000 characters")
        String message,

        boolean bookingForOther,

        String attendeeName,

        String relationship
) {

    @AssertTrue(message = "Attendee name and relationship are required when booking for another person")
    public boolean isAttendeeValid() {
        return !bookingForOther
                || (attendeeName != null && !attendeeName.isBlank()
                && relationship != null && !relationship.isBlank());
    }
}
