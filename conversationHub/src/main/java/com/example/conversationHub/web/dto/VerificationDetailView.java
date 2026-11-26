package com.example.conversationHub.web.dto;

import com.example.conversationHub.domain.enums.VerificationDecision;

import java.time.LocalDateTime;

/** Administrator-only view of verification evidence. Never returned by public endpoints. */
public record VerificationDetailView(
        Long id,
        Long providerId,
        String providerName,
        String institutionName,
        String credentialReference,
        String identityDetails,
        String qualifications,
        String documentReference,
        VerificationDecision decision,
        String decidedByName,
        LocalDateTime decidedAt,
        LocalDateTime createdAt
) {
}
