package com.example.conversationHub.web.dto;

import com.example.conversationHub.domain.enums.ServiceMode;
import com.example.conversationHub.domain.enums.VerificationStatus;

public record ProviderView(
        Long id,
        String name,
        String specialisation,
        ServiceMode serviceMode,
        VerificationStatus verificationStatus,
        Integer responseDeadlineHours,
        String cancellationRule,
        String serviceType
) {
}
