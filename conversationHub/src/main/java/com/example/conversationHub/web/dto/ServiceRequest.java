package com.example.conversationHub.web.dto;

import com.example.conversationHub.domain.enums.ServiceCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ServiceRequest(
        /** Only needed when PUT is called on /api/admin/services without an id in the path. */
        Long id,

        ServiceCategory category,

        @NotBlank(message = "Service type is required")
        @Size(max = 100, message = "Service type must not exceed 100 characters")
        String serviceType,

        Integer durationMinutes,

        @Size(max = 255, message = "Charge information must not exceed 255 characters")
        String chargeInformation,

        Boolean active
) {
}
