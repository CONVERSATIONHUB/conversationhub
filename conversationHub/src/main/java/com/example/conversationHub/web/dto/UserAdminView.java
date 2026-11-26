package com.example.conversationHub.web.dto;

import com.example.conversationHub.domain.enums.AccountType;

public record UserAdminView(
        Long id,
        String name,
        String phone,
        String email,
        AccountType accountType,
        String role,
        boolean verified,
        boolean active,
        Long institutionId,
        Long providerId
) {
}
