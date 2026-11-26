package com.example.conversationHub.web.dto;

import com.example.conversationHub.domain.enums.ServiceCategory;

public record ServiceView(
        Long id,
        ServiceCategory category,
        String serviceType,
        Integer durationMinutes,
        String chargeInformation,
        boolean active
) {
}
