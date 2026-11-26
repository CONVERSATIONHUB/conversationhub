package com.example.conversationHub.web.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AuthVerifyRequest(
        @NotNull(message = "{validation.code.required}")
        @JsonAlias("challenge_id")
        Long challengeId,

        @NotBlank(message = "{validation.code.required}")
        String code
) {
}
