package com.example.conversationHub.web.dto;

/**
 * Optional filters for the public catalogue. Values are kept as strings so that unknown
 * category or identifier values simply produce empty results instead of a binding error.
 */
public record ProviderFilter(
        String category,
        String institutionId,
        String providerId,
        String serviceType,
        String location
) {
    public static ProviderFilter empty() {
        return new ProviderFilter(null, null, null, null, null);
    }
}
