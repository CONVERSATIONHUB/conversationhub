package com.example.conversationHub.web.dto;

import com.example.conversationHub.domain.enums.VerificationStatus;

/**
 * Public, publishable provider profile. Identity, qualification and verification
 * document details are deliberately excluded.
 */
public record ProviderDetailView(
        Long id,
        String name,
        String affiliation,
        String serviceType,
        String specialisation,
        VerificationStatus verificationStatus,
        String mode,
        String location,
        String serviceCharge,
        Integer responseDeadlineHours,
        String cancellationRule,
        Integer minLeadTimeHours,
        Integer cancellationCutoffHours,
        String noShowRule,
        Integer noShowRestrictionHours,
        Integer availableSlots,
        boolean hasAvailableSlots
) {
}
