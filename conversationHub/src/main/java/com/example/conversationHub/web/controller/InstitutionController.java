package com.example.conversationHub.web.controller;

import com.example.conversationHub.service.ProviderManagementService;
import com.example.conversationHub.service.StaffService;
import com.example.conversationHub.web.dto.ApiResponse;
import com.example.conversationHub.web.dto.CreateInstitutionalProviderRequest;
import com.example.conversationHub.web.dto.CreateStaffRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/institutions")
public class InstitutionController {

    private final StaffService staffService;
    private final ProviderManagementService providerManagementService;

    public InstitutionController(StaffService staffService,
                                 ProviderManagementService providerManagementService) {
        this.staffService = staffService;
        this.providerManagementService = providerManagementService;
    }

    @PostMapping("/{institutionId}/staff")
    public ResponseEntity<ApiResponse> createStaff(@PathVariable Long institutionId,
                                                   Authentication authentication,
                                                   @Valid @RequestBody CreateStaffRequest request) {
        Long id = staffService.createStaff(institutionId, authentication.getName(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Staff account created", id));
    }

    @GetMapping("/{institutionId}/staff")
    public ApiResponse staff(@PathVariable Long institutionId, Authentication authentication) {
        return ApiResponse.ok("Institution staff retrieved",
                staffService.listStaff(institutionId, authentication.getName()));
    }

    @GetMapping("/{institutionId}/staff/pending")
    public ApiResponse pendingStaff(@PathVariable Long institutionId, Authentication authentication) {
        return ApiResponse.ok("Pending staff retrieved",
                staffService.listPendingStaff(institutionId, authentication.getName()));
    }

    @PostMapping("/{institutionId}/staff/{userId}/verify")
    public ApiResponse verifyStaff(@PathVariable Long institutionId,
                                   @PathVariable Long userId,
                                   Authentication authentication) {
        staffService.verify(institutionId, userId, authentication.getName());
        return ApiResponse.ok("Staff verified", null);
    }

    @PostMapping("/{institutionId}/staff/{userId}/reject")
    public ApiResponse rejectStaff(@PathVariable Long institutionId,
                                   @PathVariable Long userId,
                                   Authentication authentication) {
        staffService.reject(institutionId, userId, authentication.getName());
        return ApiResponse.ok("Staff verification rejected", null);
    }

    @PostMapping("/{institutionId}/providers")
    public ResponseEntity<ApiResponse> createProvider(@PathVariable Long institutionId,
                                                      Authentication authentication,
                                                      @Valid @RequestBody CreateInstitutionalProviderRequest request) {
        Long id = providerManagementService.createInstitutionalProvider(
                institutionId, authentication.getName(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Institutional provider created", id));
    }

    @GetMapping("/{institutionId}/providers")
    public ApiResponse providers(@PathVariable Long institutionId, Authentication authentication) {
        return ApiResponse.ok("Institution providers retrieved",
                providerManagementService.listInstitutionProviders(institutionId, authentication.getName()));
    }

    @PostMapping("/{institutionId}/providers/{providerId}/verify")
    public ApiResponse verifyProvider(@PathVariable Long institutionId,
                                      @PathVariable Long providerId,
                                      Authentication authentication) {
        providerManagementService.verifyInstitutionalProvider(
                institutionId, providerId, authentication.getName());
        return ApiResponse.ok("Institutional provider verified", null);
    }

    @PostMapping("/{institutionId}/providers/{providerId}/reject")
    public ApiResponse rejectProvider(@PathVariable Long institutionId,
                                      @PathVariable Long providerId,
                                      Authentication authentication) {
        providerManagementService.rejectInstitutionalProvider(
                institutionId, providerId, authentication.getName());
        return ApiResponse.ok("Institutional provider rejected", null);
    }
}
