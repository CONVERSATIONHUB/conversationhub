package com.example.conversationHub.service;

import com.example.conversationHub.web.dto.ProviderUpdateRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Self-service entry point for provider profile management. Profile state transitions and
 * audits are centralised in ProviderManagementService so that manager and provider edits
 * follow the same pending re-verification rules.
 */
@Service
@Transactional
public class ProviderProfileService {

    private final ProviderManagementService providerManagementService;

    public ProviderProfileService(ProviderManagementService providerManagementService) {
        this.providerManagementService = providerManagementService;
    }

    public void updateOwnProfile(String username, ProviderUpdateRequest request) {
        providerManagementService.updateOwnProvider(username, request);
    }

    public void deactivateOwnProvider(Long providerId, String username) {
        providerManagementService.deactivateProvider(providerId, username);
    }
}
