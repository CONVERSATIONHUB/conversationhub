package com.example.conversationHub.web.dto;

import com.example.conversationHub.domain.enums.LanguageCode;
import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterSeekerRequest(

        @NotBlank(message = "{validation.name.required}")
        @Size(max = 150, message = "Name must not exceed 150 characters")
        String name,

        @NotBlank(message = "{validation.phone.required}")
        @Pattern(regexp = "^[0-9+()\\-\\s]{7,30}$", message = "Phone number format is invalid")
        String phone,

        @Email(message = "Email format is invalid")
        @Size(max = 150, message = "Email must not exceed 150 characters")
        String email,

        @NotBlank(message = "{validation.password.required}")
        @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
        String password,

        LanguageCode languageCode,

        @AssertTrue(message = "{validation.consent.required}")
        @JsonAlias("consent")
        boolean consentGiven
) {
    /** Backwards-compatible constructor for callers that do not supply consent explicitly. */
    public RegisterSeekerRequest(String name, String phone, String email, String password,
                                 LanguageCode languageCode) {
        this(name, phone, email, password, languageCode, false);
    }
}
