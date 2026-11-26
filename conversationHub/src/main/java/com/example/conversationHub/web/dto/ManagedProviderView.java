package com.example.conversationHub.web.dto;

import com.example.conversationHub.domain.enums.ServiceCategory;
import com.example.conversationHub.domain.enums.ServiceMode;
import com.example.conversationHub.domain.enums.VerificationStatus;

public record ManagedProviderView(
        Long id,
        Long userId,
        String name,
        String phone,
        ServiceCategory category,
        String serviceType,
        ServiceMode serviceMode,
        VerificationStatus verificationStatus,
        Integer responseDeadlineHours,
        String cancellationRule,
        String institutionName,
        boolean published,
        boolean pendingReverification,
        String location,
        Integer minLeadTimeHours,
        Integer cancellationCutoffHours
) {
}
