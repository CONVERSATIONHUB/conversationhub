package com.example.conversationHub.web.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotNull;

public record UserAdminActiveRequest(
        @NotNull(message = "Active state is required")
        @JsonAlias("enabled")
        Boolean active
) {
}
