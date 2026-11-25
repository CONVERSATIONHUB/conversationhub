package com.example.conversationHub.web.controller;

import com.example.conversationHub.service.ProfileService;
import com.example.conversationHub.web.dto.ApiResponse;
import com.example.conversationHub.web.dto.LanguagePreferenceRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me")
public class MeController {

    private final ProfileService profileService;

    public MeController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping
    public ApiResponse me(Authentication authentication) {
        return ApiResponse.ok("Profile retrieved", profileService.me(authentication.getName()));
    }

    @PutMapping("/language")
    public ApiResponse updateLanguage(Authentication authentication,
                                      @Valid @RequestBody LanguagePreferenceRequest request) {
        profileService.updateLanguage(authentication.getName(), request);
        return ApiResponse.ok("Language preference updated", null);
    }
}
