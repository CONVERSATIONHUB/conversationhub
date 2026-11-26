package com.example.conversationHub.web.controller;

import com.example.conversationHub.service.SlotRecurrenceService;
import com.example.conversationHub.web.dto.ApiResponse;
import com.example.conversationHub.web.dto.RecurrenceRequest;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping({"/api/availability/providers/{providerId}/recurrences",
        "/api/providers/{providerId}/availability/recurrences"})
public class RecurrenceController {

    private final SlotRecurrenceService recurrenceService;

    public RecurrenceController(SlotRecurrenceService recurrenceService) {
        this.recurrenceService = recurrenceService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse> create(@PathVariable Long providerId,
                                              Authentication authentication,
                                              @Valid @RequestBody RecurrenceRequest request) {
        Long id = recurrenceService.createRecurrence(providerId, authentication.getName(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Recurrence created", id));
    }

    @GetMapping
    public ApiResponse list(@PathVariable Long providerId, Authentication authentication) {
        return ApiResponse.ok("Recurrences retrieved",
                recurrenceService.listRecurrences(providerId, authentication.getName()));
    }

    @DeleteMapping("/{recurrenceId}")
    public ApiResponse delete(@PathVariable Long providerId,
                              @PathVariable Long recurrenceId,
                              Authentication authentication) {
        recurrenceService.deleteRecurrence(providerId, recurrenceId, authentication.getName());
        return ApiResponse.ok("Recurrence deleted", null);
    }

    @PostMapping("/generate")
    public ApiResponse generate(@PathVariable Long providerId,
                                Authentication authentication,
                                @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.ok("Slots generated from recurrences",
                recurrenceService.generateSlots(providerId, authentication.getName(), from, to));
    }
}
