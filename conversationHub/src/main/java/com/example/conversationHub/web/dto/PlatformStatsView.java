package com.example.conversationHub.web.dto;

import java.util.Map;

public record PlatformStatsView(
        long institutionCount,
        long providerCount,
        long bookingCount,
        Map<String, Long> institutionsByStatus,
        Map<String, Long> providersByCategory,
        Map<String, Long> bookingsByCategory,
        Map<String, Long> bookingsByStatus,
        Map<String, Map<String, Long>> bookingsByCategoryAndStatus
) {
}
