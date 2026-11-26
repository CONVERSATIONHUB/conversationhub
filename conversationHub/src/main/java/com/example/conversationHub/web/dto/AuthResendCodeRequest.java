package com.example.conversationHub.web.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotNull;

public record AuthResendCodeRequest(
        @NotNull(message = "{validation.code.required}")
        @JsonAlias("challenge_id")
        Long challengeId
) {
}
