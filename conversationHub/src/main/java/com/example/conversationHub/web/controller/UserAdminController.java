package com.example.conversationHub.web.controller;

import com.example.conversationHub.service.UserAdminService;
import com.example.conversationHub.web.dto.ApiResponse;
import com.example.conversationHub.web.dto.UserAdminActiveRequest;
import com.example.conversationHub.web.dto.UserAdminRoleRequest;
import jakarta.validation.Valid;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/users")
public class UserAdminController {

    private final UserAdminService userAdminService;
    private final MessageSource messageSource;

    public UserAdminController(UserAdminService userAdminService, MessageSource messageSource) {
        this.userAdminService = userAdminService;
        this.messageSource = messageSource;
    }

    @GetMapping
    public ApiResponse listUsers() {
        return ApiResponse.ok("Users retrieved", userAdminService.listUsers());
    }

    @PostMapping("/{id}/role")
    public ApiResponse changeRole(@PathVariable("id") Long id,
                                  @Valid @RequestBody UserAdminRoleRequest request,
                                  Authentication authentication) {
        userAdminService.assignRole(id, request.accountType(), request.roleName(),
                authentication.getName());
        return ApiResponse.ok(message("notification.role_assigned"), null);
    }

    @PostMapping("/{id}/active")
    public ApiResponse setActive(@PathVariable("id") Long id,
                                 @Valid @RequestBody UserAdminActiveRequest request,
                                 Authentication authentication) {
        boolean active = Boolean.TRUE.equals(request.active());
        userAdminService.setActive(id, active, authentication.getName());
        return ApiResponse.ok(active ? "Account activated" : message("notification.account_deactivated"), null);
    }

    @GetMapping("/deletion-requests")
    public ApiResponse listDeletionRequests() {
        return ApiResponse.ok("Deletion requests retrieved",
                userAdminService.listDeletionRequests());
    }

    @PostMapping("/deletion-requests/{id}/approve")
    public ApiResponse approveDeletionRequest(@PathVariable("id") Long id, Authentication authentication) {
        userAdminService.approveDeletionRequest(id, authentication.getName());
        return ApiResponse.ok("Account anonymised", null);
    }

    @PostMapping("/deletion-requests/{id}/reject")
    public ApiResponse rejectDeletionRequest(@PathVariable("id") Long id, Authentication authentication) {
        userAdminService.rejectDeletionRequest(id, authentication.getName());
        return ApiResponse.ok(message("notification.deletion_requested"), null);
    }

    private String message(String key) {
        return messageSource.getMessage(key, null, LocaleContextHolder.getLocale());
    }
}
