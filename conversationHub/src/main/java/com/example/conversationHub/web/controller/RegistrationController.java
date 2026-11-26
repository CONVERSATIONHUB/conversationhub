package com.example.conversationHub.web.controller;

import com.example.conversationHub.domain.AppUser;
import com.example.conversationHub.service.IdentityService;
import com.example.conversationHub.service.InstitutionService;
import com.example.conversationHub.service.ProviderManagementService;
import com.example.conversationHub.service.SeekerRegistrationService;
import com.example.conversationHub.web.dto.ApiResponse;
import com.example.conversationHub.web.dto.AuthForgotPasswordRequest;
import com.example.conversationHub.web.dto.AuthPasswordResetRequest;
import com.example.conversationHub.web.dto.AuthPasswordResetResponse;
import com.example.conversationHub.web.dto.AuthRegistrationResponse;
import com.example.conversationHub.web.dto.AuthResendCodeRequest;
import com.example.conversationHub.web.dto.AuthVerifyRequest;
import com.example.conversationHub.web.dto.RegisterIndependentProviderRequest;
import com.example.conversationHub.web.dto.RegisterInstitutionRequest;
import com.example.conversationHub.web.dto.RegisterSeekerRequest;
import jakarta.validation.Valid;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class RegistrationController {

    private final SeekerRegistrationService registrationService;
    private final InstitutionService institutionService;
    private final ProviderManagementService providerManagementService;
    private final IdentityService identityService;
    private final MessageSource messageSource;

    public RegistrationController(SeekerRegistrationService registrationService,
                                  InstitutionService institutionService,
                                  ProviderManagementService providerManagementService,
                                  IdentityService identityService,
                                  MessageSource messageSource) {
        this.registrationService = registrationService;
        this.institutionService = institutionService;
        this.providerManagementService = providerManagementService;
        this.identityService = identityService;
        this.messageSource = messageSource;
    }

    @PostMapping("/register/seeker")
    public ResponseEntity<ApiResponse> registerSeeker(@Valid @RequestBody RegisterSeekerRequest request) {
        AuthRegistrationResponse result = registrationService.registerWithVerification(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(message("register.success"), result));
    }

    @PostMapping("/verify")
    public ApiResponse verify(@Valid @RequestBody AuthVerifyRequest request) {
        AppUser user = identityService.verifyChallenge(request.challengeId(), request.code());
        return ApiResponse.ok(message("auth.verified"), user.getId());
    }

    @PostMapping("/resend-code")
    public ApiResponse resendCode(@Valid @RequestBody AuthResendCodeRequest request) {
        return ApiResponse.ok(message("auth.code_resent"),
                identityService.resendCode(request.challengeId()));
    }

    @PostMapping("/forgot-password")
    public ApiResponse forgotPassword(@Valid @RequestBody AuthForgotPasswordRequest request) {
        AuthPasswordResetResponse response = identityService.forgotPassword(request.resolvedIdentifier());
        return ApiResponse.ok("Password reset token issued", response);
    }

    @PostMapping("/reset-password")
    public ApiResponse resetPassword(@Valid @RequestBody AuthPasswordResetRequest request) {
        identityService.resetPassword(request.token(), request.newPassword());
        return ApiResponse.ok(message("auth.password_reset"), null);
    }

    @PostMapping("/register/institution")
    public ResponseEntity<ApiResponse> registerInstitution(
            @Valid @RequestBody RegisterInstitutionRequest request) {
        Long id = institutionService.registerInstitution(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Institution submitted for approval", id));
    }

    @PostMapping("/register/provider/independent")
    public ResponseEntity<ApiResponse> registerIndependentProvider(
            @Valid @RequestBody RegisterIndependentProviderRequest request) {
        Long id = providerManagementService.registerIndependentProvider(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Independent provider submitted for verification", id));
    }

    private String message(String key) {
        return messageSource.getMessage(key, null, LocaleContextHolder.getLocale());
    }
}
