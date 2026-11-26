package com.example.conversationHub.web.controller;

import com.example.conversationHub.config.PersistentLocaleResolver;
import com.example.conversationHub.service.ProfileService;
import com.example.conversationHub.web.dto.ApiResponse;
import com.example.conversationHub.web.dto.AuthChallengeResponse;
import com.example.conversationHub.web.dto.ContactChangeRequest;
import com.example.conversationHub.web.dto.DeletionRequestCreateRequest;
import com.example.conversationHub.web.dto.LanguagePreferenceRequest;
import com.example.conversationHub.web.dto.VerifyContactChangeRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.LocaleResolver;

@RestController
@RequestMapping("/api/me")
public class MeController {

    private final ProfileService profileService;
    private final MessageSource messageSource;
    private final LocaleResolver localeResolver;

    public MeController(ProfileService profileService, MessageSource messageSource,
                        LocaleResolver localeResolver) {
        this.profileService = profileService;
        this.messageSource = messageSource;
        this.localeResolver = localeResolver;
    }

    @GetMapping
    public ApiResponse me(Authentication authentication) {
        return ApiResponse.ok("Profile retrieved", profileService.me(authentication.getName()));
    }

    @PutMapping("/language")
    public ApiResponse updateLanguage(Authentication authentication,
                                      @Valid @RequestBody LanguagePreferenceRequest request,
                                      HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        profileService.updateLanguage(authentication.getName(), request);
        if (httpRequest != null && httpResponse != null) {
            localeResolver.setLocale(httpRequest, httpResponse,
                    PersistentLocaleResolver.toLocale(request.languageCode()));
        }
        return ApiResponse.ok("Language preference updated", null);
    }

    public ApiResponse updateLanguage(Authentication authentication,
                                      LanguagePreferenceRequest request) {
        return updateLanguage(authentication, request, null, null);
    }

    @PostMapping("/contact")
    public ApiResponse requestContactChange(Authentication authentication,
                                            @Valid @RequestBody ContactChangeRequest request) {
        AuthChallengeResponse challenge = profileService.requestContactChange(
                authentication.getName(), request);
        return ApiResponse.ok(message("auth.code_resent"), challenge);
    }

    @PostMapping("/contact/verify")
    public ApiResponse verifyContactChange(Authentication authentication,
                                           @Valid @RequestBody VerifyContactChangeRequest request) {
        profileService.verifyContactChange(authentication.getName(),
                request.challengeId(), request.code());
        return ApiResponse.ok(message("auth.contact_updated"), null);
    }

    @PostMapping("/deletion-request")
    public ApiResponse createDeletionRequest(Authentication authentication,
                                             @Valid @RequestBody(required = false) DeletionRequestCreateRequest request) {
        String reason = request == null ? null : request.reason();
        Long id = profileService.requestDeletion(authentication.getName(), reason);
        return ApiResponse.ok(message("auth.deletion_requested"), id);
    }

    @GetMapping("/deletion-request")
    public ApiResponse getDeletionRequest(Authentication authentication) {
        return ApiResponse.ok("Deletion request retrieved",
                profileService.latestDeletionRequest(authentication.getName()));
    }

    @GetMapping("/deletion-requests")
    public ApiResponse listDeletionRequests(Authentication authentication) {
        return ApiResponse.ok("Deletion requests retrieved",
                profileService.deletionRequests(authentication.getName()));
    }

    private String message(String key) {
        return messageSource.getMessage(key, null, LocaleContextHolder.getLocale());
    }
}
