package com.example.conversationHub.web.dto;

import java.util.Map;

public record InstitutionStatsView(
        Long institutionId,
        String institutionName,
        long requestCount,
        long approvalCount,
        long rejectionCount,
        long noShowCount,
        long pendingCount,
        Map<String, Long> bookingsByCategory,
        Map<String, Long> bookingsByStatus
) {
}
