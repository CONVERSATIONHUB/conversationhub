package com.example.conversationHub.web.controller;

import com.example.conversationHub.service.AvailabilityService;
import com.example.conversationHub.web.dto.ApiResponse;
import com.example.conversationHub.web.dto.SlotBlockRequest;
import com.example.conversationHub.web.dto.SlotRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/providers/{providerId}/availability")
public class AvailabilityController {

    private final AvailabilityService availabilityService;

    public AvailabilityController(AvailabilityService availabilityService) {
        this.availabilityService = availabilityService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse> create(@PathVariable Long providerId,
                                              Authentication authentication,
                                              @Valid @RequestBody SlotRequest request) {
        Long id = availabilityService.createSlot(providerId, authentication.getName(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Slot created", id));
    }

    @GetMapping
    public ApiResponse list(@PathVariable Long providerId, Authentication authentication) {
        return ApiResponse.ok("Slots retrieved",
                availabilityService.listProviderSlots(providerId, authentication.getName()));
    }

    @PutMapping("/{slotId}")
    public ApiResponse update(@PathVariable Long providerId,
                              @PathVariable Long slotId,
                              Authentication authentication,
                              @Valid @RequestBody SlotRequest request) {
        availabilityService.updateSlot(providerId, slotId, authentication.getName(), request);
        return ApiResponse.ok("Slot updated", null);
    }

    @PatchMapping("/{slotId}/block")
    public ApiResponse block(@PathVariable Long providerId,
                             @PathVariable Long slotId,
                             Authentication authentication,
                             @Valid @RequestBody SlotBlockRequest request) {
        availabilityService.setBlocked(providerId, slotId, authentication.getName(), request.blocked());
        return ApiResponse.ok(request.blocked() ? "Slot blocked" : "Slot unblocked", null);
    }
}
