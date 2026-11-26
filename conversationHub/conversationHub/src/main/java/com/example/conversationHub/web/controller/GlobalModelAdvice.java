package com.example.conversationHub.web.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalModelAdvice {

    @ModelAttribute("currentUri")
    public String currentUri(HttpServletRequest request) {
        return request.getRequestURI();
    }

    @ModelAttribute("currentLang")
    public String currentLang(HttpServletRequest request) {
        Object locale = request.getAttribute(org.springframework.web.servlet.DispatcherServlet.LOCALE_RESOLVER_ATTRIBUTE);
        if (locale instanceof org.springframework.web.servlet.LocaleResolver resolver) {
            return resolver.resolveLocale(request).getLanguage();
        }
        return request.getLocale().getLanguage();
    }
}
