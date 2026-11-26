package com.example.conversationHub.web.controller;

import com.example.conversationHub.domain.enums.LanguageCode;
import com.example.conversationHub.service.SystemSettingsService;
import com.example.conversationHub.web.dto.ApiResponse;
import com.example.conversationHub.web.dto.SettingsLanguageRequest;
import com.example.conversationHub.web.dto.SettingsLanguageView;
import jakarta.validation.Valid;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/settings")
public class SettingsAdminController {

    private final SystemSettingsService systemSettingsService;
    private final MessageSource messageSource;

    public SettingsAdminController(SystemSettingsService systemSettingsService,
                                   MessageSource messageSource) {
        this.systemSettingsService = systemSettingsService;
        this.messageSource = messageSource;
    }

    @GetMapping("/language")
    public ApiResponse getLanguage() {
        return ApiResponse.ok("Default language retrieved",
                new SettingsLanguageView(systemSettingsService.getDefaultLanguage()));
    }

    @PutMapping("/language")
    public ApiResponse updateLanguage(@Valid @RequestBody SettingsLanguageRequest request,
                                      Authentication authentication) {
        LanguageCode languageCode = request.languageCode();
        systemSettingsService.setDefaultLanguage(languageCode, authentication.getName());
        return ApiResponse.ok(message("notification.settings_updated"),
                new SettingsLanguageView(languageCode));
    }

    private String message(String key) {
        return messageSource.getMessage(key, null, LocaleContextHolder.getLocale());
    }
}
