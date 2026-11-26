package com.example.conversationHub.web.controller;

import com.example.conversationHub.service.PublicCatalogService;
import com.example.conversationHub.web.dto.ApiResponse;
import com.example.conversationHub.web.dto.ProviderFilter;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public")
public class PublicController {

    private final PublicCatalogService catalogService;

    public PublicController(PublicCatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping("/services")
    public ApiResponse services(@RequestParam(required = false) String category,
                                @RequestParam(required = false) String institutionId,
                                @RequestParam(required = false) String providerId,
                                @RequestParam(required = false) String serviceType,
                                @RequestParam(required = false) String location) {
        ProviderFilter filter = new ProviderFilter(
                category, institutionId, providerId, serviceType, location);
        return ApiResponse.ok("Services retrieved", catalogService.listServices(filter));
    }

    @GetMapping("/institutions")
    public ApiResponse institutions() {
        return ApiResponse.ok("Approved institutions retrieved", catalogService.listApprovedInstitutions());
    }

    @GetMapping("/providers")
    public ApiResponse providers(@RequestParam(required = false) String category,
                                 @RequestParam(required = false) String institutionId,
                                 @RequestParam(required = false) String providerId,
                                 @RequestParam(required = false) String serviceType,
                                 @RequestParam(required = false) String location) {
        ProviderFilter filter = new ProviderFilter(
                category, institutionId, providerId, serviceType, location);
        return ApiResponse.ok("Verified providers retrieved", catalogService.listVerifiedProviders(filter));
    }

    @GetMapping("/providers/{providerId}")
    public ApiResponse provider(@PathVariable Long providerId) {
        return ApiResponse.ok("Provider retrieved", catalogService.getProviderDetail(providerId));
    }

    @GetMapping("/providers/{providerId}/slots")
    public ApiResponse slots(@PathVariable Long providerId) {
        return ApiResponse.ok("Available slots retrieved", catalogService.listAvailableSlots(providerId));
    }
}
