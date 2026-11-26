package com.example.conversationHub.config;

import com.example.conversationHub.repository.LanguagePreferenceRepository;
import com.example.conversationHub.service.CurrentUserService;
import com.example.conversationHub.service.ProfileService;
import com.example.conversationHub.service.SystemSettingsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;

@Configuration
public class LocaleConfig implements WebMvcConfigurer {

    private final CurrentUserService currentUserService;
    private final LanguagePreferenceRepository languagePreferenceRepository;
    private final SystemSettingsService systemSettingsService;
    private final ProfileService profileService;

    public LocaleConfig(CurrentUserService currentUserService,
                        LanguagePreferenceRepository languagePreferenceRepository,
                        SystemSettingsService systemSettingsService,
                        ProfileService profileService) {
        this.currentUserService = currentUserService;
        this.languagePreferenceRepository = languagePreferenceRepository;
        this.systemSettingsService = systemSettingsService;
        this.profileService = profileService;
    }

    @Bean
    public LocaleResolver localeResolver() {
        return new PersistentLocaleResolver(currentUserService,
                languagePreferenceRepository, systemSettingsService);
    }

    @Bean
    public LocaleChangeInterceptor localeChangeInterceptor() {
        return new PersistentLocaleChangeInterceptor(profileService);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(localeChangeInterceptor());
    }
}
