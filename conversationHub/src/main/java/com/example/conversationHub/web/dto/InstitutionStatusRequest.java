package com.example.conversationHub.web.dto;

import com.example.conversationHub.domain.enums.InstitutionStatus;
import jakarta.validation.constraints.NotNull;

public record InstitutionStatusRequest(
        @NotNull(message = "Status is required")
        InstitutionStatus status
) {
}
