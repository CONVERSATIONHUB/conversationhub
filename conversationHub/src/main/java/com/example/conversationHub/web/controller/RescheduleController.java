package com.example.conversationHub.web.controller;

import com.example.conversationHub.service.BookingService;
import com.example.conversationHub.web.dto.ApiResponse;
import com.example.conversationHub.web.dto.DecisionRequest;
import com.example.conversationHub.web.dto.RescheduleRequestDto;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/bookings")
public class RescheduleController {

    private final BookingService bookingService;

    public RescheduleController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping("/{bookingId}/reschedule")
    public ResponseEntity<ApiResponse> requestReschedule(@PathVariable Long bookingId,
                                                         Authentication authentication,
                                                         @Valid @RequestBody RescheduleRequestDto request) {
        Long id = bookingService.requestReschedule(bookingId, authentication.getName(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Reschedule request submitted", id));
    }

    @GetMapping("/reschedules/mine")
    public ApiResponse myReschedules(Authentication authentication) {
        return ApiResponse.ok("Your reschedule requests retrieved",
                bookingService.myReschedules(authentication.getName()));
    }

    @PostMapping("/reschedules/{rescheduleId}/approve")
    public ApiResponse approve(@PathVariable Long rescheduleId,
                               Authentication authentication,
                               @RequestBody(required = false) DecisionRequest request) {
        bookingService.approveReschedule(rescheduleId, authentication.getName(), request);
        return ApiResponse.ok("Reschedule approved", null);
    }

    @PostMapping("/reschedules/{rescheduleId}/reject")
    public ApiResponse reject(@PathVariable Long rescheduleId,
                              Authentication authentication,
                              @RequestBody(required = false) DecisionRequest request) {
        bookingService.rejectReschedule(rescheduleId, authentication.getName(), request);
        return ApiResponse.ok("Reschedule rejected", null);
    }
}
