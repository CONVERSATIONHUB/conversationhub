package com.example.conversationHub.web.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

/** Accepts a single identifier or explicit phone/email (release 1 supports both channels). */
public record AuthForgotPasswordRequest(
        @JsonAlias({"username", "contact"})
        String identifier,

        String phone,

        String email
) {
    public String resolvedIdentifier() {
        if (identifier != null && !identifier.isBlank()) {
            return identifier.trim();
        }
        if (phone != null && !phone.isBlank()) {
            return phone.trim();
        }
        if (email != null && !email.isBlank()) {
            return email.trim();
        }
        return null;
    }
}
