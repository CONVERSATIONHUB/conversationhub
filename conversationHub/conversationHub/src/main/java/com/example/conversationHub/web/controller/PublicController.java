package com.example.conversationHub.web.controller;

import com.example.conversationHub.service.PublicCatalogService;
import com.example.conversationHub.web.dto.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public")
public class PublicController {

    private final PublicCatalogService catalogService;

    public PublicController(PublicCatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping("/services")
    public ApiResponse services() {
        return ApiResponse.ok("Services retrieved", catalogService.listServices());
    }

    @GetMapping("/institutions")
    public ApiResponse institutions() {
        return ApiResponse.ok("Approved institutions retrieved", catalogService.listApprovedInstitutions());
    }

    @GetMapping("/providers")
    public ApiResponse providers() {
        return ApiResponse.ok("Verified providers retrieved", catalogService.listVerifiedProviders());
    }

    @GetMapping("/providers/{providerId}/slots")
    public ApiResponse slots(@PathVariable Long providerId) {
        return ApiResponse.ok("Available slots retrieved", catalogService.listAvailableSlots(providerId));
    }
}
