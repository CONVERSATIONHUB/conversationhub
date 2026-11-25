package com.example.conversationHub.web.dto;

import com.example.conversationHub.domain.enums.LanguageCode;
import jakarta.validation.constraints.NotNull;

public record LanguagePreferenceRequest(
        @NotNull(message = "Language is required")
        LanguageCode languageCode
) {
}
