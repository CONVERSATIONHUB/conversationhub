package com.example.conversationHub.config;

import com.example.conversationHub.domain.AppUser;
import com.example.conversationHub.domain.LanguagePreference;
import com.example.conversationHub.domain.enums.LanguageCode;
import com.example.conversationHub.repository.LanguagePreferenceRepository;
import com.example.conversationHub.service.CurrentUserService;
import com.example.conversationHub.service.SystemSettingsService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.LocaleResolver;

import java.io.IOException;

/** Applies the stored language preference to the session as soon as login succeeds. */
@Component
public class LocaleAwareAuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private static final Logger log = LoggerFactory.getLogger(LocaleAwareAuthenticationSuccessHandler.class);

    private final CurrentUserService currentUserService;
    private final LanguagePreferenceRepository languagePreferenceRepository;
    private final SystemSettingsService systemSettingsService;
    private final LocaleResolver localeResolver;

    public LocaleAwareAuthenticationSuccessHandler(CurrentUserService currentUserService,
                                                   LanguagePreferenceRepository languagePreferenceRepository,
                                                   SystemSettingsService systemSettingsService,
                                                   LocaleResolver localeResolver) {
        this.currentUserService = currentUserService;
        this.languagePreferenceRepository = languagePreferenceRepository;
        this.systemSettingsService = systemSettingsService;
        this.localeResolver = localeResolver;
        setDefaultTargetUrl("/dashboard");
        setAlwaysUseDefaultTargetUrl(true);
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        try {
            AppUser user = currentUserService.require(authentication.getName());
            LanguageCode code = languagePreferenceRepository.findByUserId(user.getId())
                    .map(LanguagePreference::getLanguageCode)
                    .orElseGet(systemSettingsService::getDefaultLanguage);
            localeResolver.setLocale(request, response, PersistentLocaleResolver.toLocale(code));
        } catch (RuntimeException ex) {
            log.debug("Could not apply language preference on login for {}", authentication.getName(), ex);
        }
        super.onAuthenticationSuccess(request, response, authentication);
    }
}
