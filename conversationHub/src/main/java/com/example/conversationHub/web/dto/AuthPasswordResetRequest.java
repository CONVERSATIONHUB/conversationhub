package com.example.conversationHub.web.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;

public record AuthPasswordResetRequest(
        @NotBlank(message = "{validation.token.required}")
        @JsonAlias({"resetToken", "reset_token"})
        String token,

        @NotBlank(message = "{validation.password.required}")
        @JsonAlias("password")
        String newPassword
) {
}
