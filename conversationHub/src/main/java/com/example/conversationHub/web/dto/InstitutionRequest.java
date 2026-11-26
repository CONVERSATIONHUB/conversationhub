package com.example.conversationHub.web.dto;

import com.example.conversationHub.domain.enums.InstitutionType;
import jakarta.validation.constraints.Email;
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
        String location,

        @Size(max = 30, message = "Contact phone must not exceed 30 characters")
        String contactPhone,

        @Email(message = "Contact email format is invalid")
        @Size(max = 150, message = "Contact email must not exceed 150 characters")
        String contactEmail,

        @Size(max = 255, message = "Contact address must not exceed 255 characters")
        String contactAddress
) {
}
