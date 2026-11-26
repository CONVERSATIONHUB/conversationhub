package com.example.conversationHub.web.dto;

import jakarta.validation.constraints.Size;

public record DeletionRequestCreateRequest(
        @Size(max = 500, message = "Reason must not exceed 500 characters")
        String reason
) {
}
