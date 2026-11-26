package com.example.conversationHub.web.dto;

import com.example.conversationHub.domain.enums.LanguageCode;
import com.example.conversationHub.domain.enums.ServiceMode;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterIndependentProviderRequest(
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
        LanguageCode languageCode,

        @NotNull(message = "Service is required")
        Long serviceId,

        @Size(max = 200, message = "Specialisation must not exceed 200 characters")
        String specialisation,

        @NotNull(message = "Service mode is required")
        ServiceMode serviceMode,

        @NotNull(message = "Response deadline is required")
        @Min(value = 1, message = "Response deadline must be at least 1 hour")
        Integer responseDeadlineHours,

        @NotBlank(message = "Cancellation rule is required")
        @Size(max = 500, message = "Cancellation rule must not exceed 500 characters")
        String cancellationRule
) {
}
