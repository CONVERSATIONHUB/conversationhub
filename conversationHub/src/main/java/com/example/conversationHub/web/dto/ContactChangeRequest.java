package com.example.conversationHub.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ContactChangeRequest(
        @Pattern(regexp = "^[0-9+()\\-\\s]{7,30}$", message = "Phone number format is invalid")
        String phone,

        @Email(message = "Email format is invalid")
        @Size(max = 150, message = "Email must not exceed 150 characters")
        String email
) {
}
