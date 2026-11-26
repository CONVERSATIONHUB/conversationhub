package com.example.conversationHub.web.dto;

import com.example.conversationHub.domain.enums.LanguageCode;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterSeekerRequest(

        @NotBlank(message = "Name is required")
        @Size(max = 150, message = "Name must not exceed 150 characters")
        String name,

        @NotBlank(message = "Phone number is required")
        @Pattern(regexp = "^[0-9+()\\-\\s]{7,30}$", message = "Phone number format is invalid")
        String phone,

        @Email(message = "Email format is invalid")
        @Size(max = 150, message = "Email must not exceed 150 characters")
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
        String password,

        @NotNull(message = "Language is required")
        LanguageCode languageCode
) {
}
