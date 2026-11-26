package com.example.conversationHub.config;

import com.example.conversationHub.domain.AppUser;
import com.example.conversationHub.domain.LanguagePreference;
import com.example.conversationHub.domain.enums.LanguageCode;
import com.example.conversationHub.repository.LanguagePreferenceRepository;
import com.example.conversationHub.service.CurrentUserService;
import com.example.conversationHub.service.SystemSettingsService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.i18n.SessionLocaleResolver;

import java.util.Locale;

/**
 * Session locale resolver that remembers a signed-in user's language preference (FR-11.3).
 * An explicit {@code ?lang=...} switch still wins because LocaleChangeInterceptor writes
 * straight to the session attribute.
 */
public class PersistentLocaleResolver extends SessionLocaleResolver {

    private static final Logger log = LoggerFactory.getLogger(PersistentLocaleResolver.class);

    private final CurrentUserService currentUserService;
    private final LanguagePreferenceRepository languagePreferenceRepository;
    private final SystemSettingsService systemSettingsService;

    public PersistentLocaleResolver(CurrentUserService currentUserService,
                                    LanguagePreferenceRepository languagePreferenceRepository,
                                    SystemSettingsService systemSettingsService) {
        this.currentUserService = currentUserService;
        this.languagePreferenceRepository = languagePreferenceRepository;
        this.systemSettingsService = systemSettingsService;
    }

    @Override
    public Locale resolveLocale(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        Locale sessionLocale = session == null ? null
                : (Locale) session.getAttribute(LOCALE_SESSION_ATTRIBUTE_NAME);
        if (sessionLocale != null) {
            return sessionLocale;
        }

        Locale locale = toLocale(defaultLanguage());
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof org.springframework.security.core.userdetails.UserDetails) {
            try {
                AppUser user = currentUserService.require(authentication.getName());
                LanguageCode code = languagePreferenceRepository.findByUserId(user.getId())
                        .map(LanguagePreference::getLanguageCode)
                        .orElseGet(this::defaultLanguage);
                locale = toLocale(code);
                // Load the preference once and keep it in the session for later requests.
                setLocale(request, null, locale);
            } catch (RuntimeException ex) {
                log.debug("Could not load language preference for {}", authentication.getName(), ex);
            }
        }
        return locale;
    }

    private LanguageCode defaultLanguage() {
        try {
            return systemSettingsService.getDefaultLanguage();
        } catch (RuntimeException ex) {
            log.debug("Could not load the configured default language", ex);
            return LanguageCode.EN;
        }
    }

    public static Locale toLocale(LanguageCode code) {
        return code == LanguageCode.SW ? Locale.forLanguageTag("sw") : Locale.ENGLISH;
    }
}
