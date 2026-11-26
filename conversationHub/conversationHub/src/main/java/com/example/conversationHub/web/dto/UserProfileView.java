package com.example.conversationHub.web.dto;

import com.example.conversationHub.domain.enums.AccountType;
import com.example.conversationHub.domain.enums.LanguageCode;

public record UserProfileView(
        Long id,
        String name,
        String phone,
        String email,
        AccountType accountType,
        boolean verified,
        Long institutionId,
        Long providerId,
        LanguageCode languageCode
) {
}
