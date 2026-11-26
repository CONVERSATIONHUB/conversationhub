package com.example.conversationHub.web.dto;

import com.example.conversationHub.domain.enums.AccountType;
import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotNull;

public record UserAdminRoleRequest(
        @NotNull(message = "Account type is required")
        AccountType accountType,

        @JsonAlias("role")
        String roleName
) {
}
