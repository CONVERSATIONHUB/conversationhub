package com.example.conversationHub.web.controller;

import com.example.conversationHub.domain.enums.InstitutionStatus;
import com.example.conversationHub.service.AdminQueryService;
import com.example.conversationHub.service.InstitutionService;
import com.example.conversationHub.service.ProviderManagementService;
import com.example.conversationHub.web.dto.ApiResponse;
import com.example.conversationHub.web.dto.InstitutionStatusRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final InstitutionService institutionService;
    private final ProviderManagementService providerManagementService;
    private final AdminQueryService adminQueryService;

    public AdminController(InstitutionService institutionService,
                           ProviderManagementService providerManagementService,
                           AdminQueryService adminQueryService) {
        this.institutionService = institutionService;
        this.providerManagementService = providerManagementService;
        this.adminQueryService = adminQueryService;
    }

    @GetMapping("/institutions")
    public ApiResponse institutions(@RequestParam(required = false) InstitutionStatus status) {
        if (status == null) {
            return ApiResponse.ok("Institutions retrieved",
                    institutionService.listByStatus(InstitutionStatus.APPROVED));
        }
        return ApiResponse.ok("Institutions retrieved", institutionService.listByStatus(status));
    }

    @GetMapping("/institutions/pending")
    public ApiResponse pendingInstitutions() {
        return ApiResponse.ok("Pending institutions retrieved", institutionService.listPending());
    }

    @PostMapping("/institutions/{id}/approve")
    public ApiResponse approveInstitution(@PathVariable Long id, Authentication authentication) {
        institutionService.approve(id, authentication.getName());
        return ApiResponse.ok("Institution approved", null);
    }

    @PostMapping("/institutions/{id}/reject")
    public ApiResponse rejectInstitution(@PathVariable Long id, Authentication authentication) {
        institutionService.reject(id, authentication.getName());
        return ApiResponse.ok("Institution rejected", null);
    }

    @PutMapping("/institutions/{id}/status")
    public ApiResponse changeInstitutionStatus(@PathVariable Long id,
                                               @Valid @RequestBody InstitutionStatusRequest request,
                                               Authentication authentication) {
        institutionService.changeStatus(id, request.status(), authentication.getName());
        return ApiResponse.ok("Institution status updated", null);
    }

    @GetMapping("/providers/pending")
    public ApiResponse pendingProviders(Authentication authentication) {
        return ApiResponse.ok("Pending independent providers retrieved",
                providerManagementService.listPendingIndependentProviders(authentication.getName()));
    }

    @PostMapping("/providers/{id}/verify")
    public ApiResponse verifyProvider(@PathVariable Long id, Authentication authentication) {
        providerManagementService.verifyIndependentProvider(id, authentication.getName());
        return ApiResponse.ok("Independent provider verified", null);
    }

    @PostMapping("/providers/{id}/reject")
    public ApiResponse rejectProvider(@PathVariable Long id, Authentication authentication) {
        providerManagementService.rejectIndependentProvider(id, authentication.getName());
        return ApiResponse.ok("Independent provider rejected", null);
    }

    @GetMapping("/bookings")
    public ApiResponse bookings(Authentication authentication) {
        return ApiResponse.ok("Bookings retrieved",
                adminQueryService.allBookings(authentication.getName()));
    }

    @GetMapping("/audit-logs")
    public ApiResponse auditLogs(Authentication authentication) {
        return ApiResponse.ok("Audit logs retrieved",
                adminQueryService.recentAuditLogs(authentication.getName()));
    }
}
