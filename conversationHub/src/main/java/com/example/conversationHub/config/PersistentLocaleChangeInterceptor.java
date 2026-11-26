package com.example.conversationHub.config;

import com.example.conversationHub.domain.enums.LanguageCode;
import com.example.conversationHub.service.ProfileService;
import com.example.conversationHub.web.dto.LanguagePreferenceRequest;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;

/** Keeps an explicit {@code ?lang=...} switch in step with the stored preference (FR-11.2/11.3). */
public class PersistentLocaleChangeInterceptor extends LocaleChangeInterceptor {

    private static final Logger log = LoggerFactory.getLogger(PersistentLocaleChangeInterceptor.class);

    private final ProfileService profileService;

    public PersistentLocaleChangeInterceptor(ProfileService profileService) {
        this.profileService = profileService;
        setParamName("lang");
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws ServletException {
        boolean proceed = super.preHandle(request, response, handler);
        String lang = request.getParameter(getParamName());
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (proceed && lang != null && authentication != null && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof org.springframework.security.core.userdetails.UserDetails) {
            try {
                LanguageCode code = LanguageCode.valueOf(lang.trim().toUpperCase());
                profileService.updateLanguage(authentication.getName(),
                        new LanguagePreferenceRequest(code));
            } catch (RuntimeException ex) {
                log.debug("Could not persist language switch for {}", authentication.getName(), ex);
            }
        }
        return proceed;
    }
}
