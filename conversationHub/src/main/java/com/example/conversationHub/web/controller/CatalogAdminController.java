package com.example.conversationHub.web.controller;

import com.example.conversationHub.service.ProviderManagementService;
import com.example.conversationHub.web.dto.ApiResponse;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class CatalogAdminController {

    private final ProviderManagementService providerManagementService;

    public CatalogAdminController(ProviderManagementService providerManagementService) {
        this.providerManagementService = providerManagementService;
    }

    /** Administrator-only access to identity and qualification evidence. */
    @GetMapping("/providers/{providerId}/verification")
    public ApiResponse verification(@PathVariable Long providerId, Authentication authentication) {
        return ApiResponse.ok("Verification details retrieved",
                providerManagementService.verificationDetail(providerId, authentication.getName()));
    }
}
