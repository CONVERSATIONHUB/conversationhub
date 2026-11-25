package com.example.conversationHub.web.dto;

import com.example.conversationHub.domain.enums.InstitutionStatus;
import com.example.conversationHub.domain.enums.InstitutionType;

public record InstitutionView(
        Long id,
        String name,
        InstitutionType type,
        String location,
        InstitutionStatus status,
        String managerName
) {
}
