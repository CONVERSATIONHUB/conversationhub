package com.example.conversationHub.web.dto;

import com.example.conversationHub.domain.enums.InstitutionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record InstitutionRequest(
        @NotBlank(message = "Institution name is required")
        @Size(max = 200, message = "Institution name must not exceed 200 characters")
        String name,

        @NotNull(message = "Institution type is required")
        InstitutionType type,

        @Size(max = 200, message = "Location must not exceed 200 characters")
        String location
) {
}
