package com.example.conversationHub.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Enforces HTTPS without Spring Security's legacy {@code requiresChannel} API, which is no
 * longer available in Spring Security 7. The filter is disabled by default so local HTTP
 * development works; production deployments should enable it when TLS is terminated at the
 * application or at a trusted reverse proxy.
 */
public class HttpsEnforcementFilter extends OncePerRequestFilter {

    private final boolean requireHttps;

    public HttpsEnforcementFilter(boolean requireHttps) {
        this.requireHttps = requireHttps;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (requireHttps && !isSecure(request)) {
            response.sendRedirect(httpsUrl(request));
            return;
        }
        filterChain.doFilter(request, response);
    }

    private boolean isSecure(HttpServletRequest request) {
        if (request.isSecure()) {
            return true;
        }
        String forwardedProto = request.getHeader("X-Forwarded-Proto");
        return forwardedProto != null && forwardedProto.toLowerCase().contains("https");
    }

    private String httpsUrl(HttpServletRequest request) {
        StringBuffer url = request.getRequestURL();
        String value = url.toString();
        if (value.startsWith("http://")) {
            value = "https://" + value.substring("http://".length());
        }
        String query = request.getQueryString();
        if (query != null && !query.isBlank()) {
            value = value + "?" + query;
        }
        return value;
    }
}
