package com.example.conversationHub.web.controller;

import com.example.conversationHub.service.PublicCatalogService;
import com.example.conversationHub.web.dto.ApiResponse;
import com.example.conversationHub.web.dto.ServiceRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/services")
public class ServiceController {

    private final PublicCatalogService catalogService;

    public ServiceController(PublicCatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping
    public ApiResponse services(Authentication authentication) {
        return ApiResponse.ok("Service catalogue retrieved", catalogService.listAllServices());
    }

    @PostMapping
    public ResponseEntity<ApiResponse> create(@Valid @RequestBody ServiceRequest request,
                                              Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Service type created",
                catalogService.createService(request, authentication.getName())));
    }

    /** Accepts the service id in the body as well as in the path for simple admin clients. */
    @PutMapping
    public ApiResponse update(@Valid @RequestBody ServiceRequest request,
                              Authentication authentication) {
        return ApiResponse.ok("Service type updated",
                catalogService.updateService(null, request, authentication.getName()));
    }

    @PutMapping("/{id}")
    public ApiResponse updateById(@PathVariable Long id,
                                  @Valid @RequestBody ServiceRequest request,
                                  Authentication authentication) {
        return ApiResponse.ok("Service type updated",
                catalogService.updateService(id, request, authentication.getName()));
    }
}
