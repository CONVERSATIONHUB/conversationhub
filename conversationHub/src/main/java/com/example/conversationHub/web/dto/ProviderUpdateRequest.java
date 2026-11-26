package com.example.conversationHub.web.dto;

import com.example.conversationHub.domain.enums.ServiceMode;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/**
 * Partial provider profile update. Changed verified fields are held for re-verification;
 * policy fields (deadlines, rules) take effect immediately because they are captured per request.
 */
public record ProviderUpdateRequest(
        @Size(max = 150, message = "Name must not exceed 150 characters")
        String name,

        @Size(max = 200, message = "Specialisation must not exceed 200 characters")
        String specialisation,

        @Size(max = 2000, message = "Identity details must not exceed 2000 characters")
        String identityDetails,

        @Size(max = 2000, message = "Qualifications must not exceed 2000 characters")
        String qualifications,

        Long serviceId,

        ServiceMode serviceMode,

        @Size(max = 200, message = "Location must not exceed 200 characters")
        String location,

        @Min(value = 1, message = "Response deadline must be at least 1 hour")
        Integer responseDeadlineHours,

        @Size(max = 500, message = "Cancellation rule must not exceed 500 characters")
        String cancellationRule,

        @Min(value = 0, message = "Minimum lead time cannot be negative")
        Integer minLeadTimeHours,

        @Min(value = 0, message = "Cancellation cutoff cannot be negative")
        Integer cancellationCutoffHours,

        @Size(max = 500, message = "No-show rule must not exceed 500 characters")
        String noShowRule,

        @Min(value = 0, message = "No-show restriction cannot be negative")
        Integer noShowRestrictionHours
) {
}
