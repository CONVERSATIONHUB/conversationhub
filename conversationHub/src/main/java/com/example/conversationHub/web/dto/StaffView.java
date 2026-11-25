package com.example.conversationHub.web.dto;

import com.example.conversationHub.domain.enums.AccountType;

public record StaffView(
        Long id,
        String name,
        String phone,
        String email,
        AccountType accountType,
        boolean verified
) {
}
