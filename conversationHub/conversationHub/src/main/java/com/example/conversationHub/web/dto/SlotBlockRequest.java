package com.example.conversationHub.web.dto;

import jakarta.validation.constraints.NotNull;

public record SlotBlockRequest(
        @NotNull(message = "Blocked value is required")
        Boolean blocked
) {
}
