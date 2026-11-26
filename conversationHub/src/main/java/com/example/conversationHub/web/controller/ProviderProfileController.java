package com.example.conversationHub.web.controller;

import com.example.conversationHub.service.ProviderProfileService;
import com.example.conversationHub.web.dto.ApiResponse;
import com.example.conversationHub.web.dto.ProviderUpdateRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/providers")
public class ProviderProfileController {

    private final ProviderProfileService providerProfileService;

    public ProviderProfileController(ProviderProfileService providerProfileService) {
        this.providerProfileService = providerProfileService;
    }

    @PutMapping("/me")
    public ApiResponse updateMe(Authentication authentication,
                                @Valid @RequestBody ProviderUpdateRequest request) {
        providerProfileService.updateOwnProfile(authentication.getName(), request);
        return ApiResponse.ok("Provider profile updated", null);
    }

    @PostMapping("/{providerId}/deactivate")
    public ApiResponse deactivate(@PathVariable Long providerId, Authentication authentication) {
        providerProfileService.deactivateOwnProvider(providerId, authentication.getName());
        return ApiResponse.ok("Provider deactivated", null);
    }
}
