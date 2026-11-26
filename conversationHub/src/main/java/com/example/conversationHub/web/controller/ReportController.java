package com.example.conversationHub.web.controller;

import com.example.conversationHub.service.ReportingService;
import com.example.conversationHub.web.dto.ApiResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ReportController {

    private static final String CSV_CONTENT_TYPE = "text/csv; charset=UTF-8";

    private final ReportingService reportingService;

    public ReportController(ReportingService reportingService) {
        this.reportingService = reportingService;
    }

    @GetMapping("/api/admin/reports/platform")
    public ApiResponse platform(Authentication authentication) {
        return ApiResponse.ok("Platform statistics retrieved",
                reportingService.platformStats(authentication.getName()));
    }

    @GetMapping(value = "/api/admin/reports/bookings.csv", produces = CSV_CONTENT_TYPE)
    public ResponseEntity<String> platformCsv(Authentication authentication) {
        return csv(reportingService.platformBookingsCsv(authentication.getName()));
    }

    @GetMapping({"/api/institutions/{id}/reports", "/api/reports/institutions/{id}"})
    public ApiResponse institution(@PathVariable Long id, Authentication authentication) {
        return ApiResponse.ok("Institution statistics retrieved",
                reportingService.institutionStats(id, authentication.getName()));
    }

    @GetMapping(value = {"/api/institutions/{id}/reports/bookings.csv",
            "/api/reports/institutions/{id}/bookings.csv"}, produces = CSV_CONTENT_TYPE)
    public ResponseEntity<String> institutionCsv(@PathVariable Long id,
                                                 Authentication authentication) {
        return csv(reportingService.institutionBookingsCsv(id, authentication.getName()));
    }

    private ResponseEntity<String> csv(String body) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(CSV_CONTENT_TYPE))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"bookings.csv\"")
                .body(body);
    }
}
