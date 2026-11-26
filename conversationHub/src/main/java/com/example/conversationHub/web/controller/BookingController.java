package com.example.conversationHub.web.controller;

import com.example.conversationHub.service.BookingService;
import com.example.conversationHub.web.dto.ApiResponse;
import com.example.conversationHub.web.dto.CancelRequest;
import com.example.conversationHub.web.dto.CreateBookingRequest;
import com.example.conversationHub.web.dto.DecisionRequest;
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
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse> requestBooking(Authentication authentication,
                                                      @Valid @RequestBody CreateBookingRequest request) {
        Long bookingId = bookingService.requestBooking(authentication.getName(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Booking request submitted", bookingId));
    }

    @GetMapping("/mine")
    public ApiResponse myBookings(Authentication authentication) {
        return ApiResponse.ok("Your bookings retrieved",
                bookingService.myBookings(authentication.getName()));
    }

    @GetMapping("/provider")
    public ApiResponse providerBookings(Authentication authentication) {
        return ApiResponse.ok("Provider bookings retrieved",
                bookingService.providerBookings(authentication.getName()));
    }

    @GetMapping("/institution")
    public ApiResponse institutionBookings(Authentication authentication) {
        return ApiResponse.ok("Institution bookings retrieved",
                bookingService.institutionBookings(authentication.getName()));
    }

    @GetMapping("/{bookingId}")
    public ApiResponse booking(@PathVariable Long bookingId, Authentication authentication) {
        return ApiResponse.ok("Booking retrieved",
                bookingService.getBooking(bookingId, authentication.getName()));
    }

    @PostMapping("/{bookingId}/approve")
    public ApiResponse approve(@PathVariable Long bookingId,
                               Authentication authentication,
                               @RequestBody(required = false) DecisionRequest request) {
        bookingService.approve(bookingId, authentication.getName(), request);
        return ApiResponse.ok("Booking approved", null);
    }

    @PostMapping("/{bookingId}/reject")
    public ApiResponse reject(@PathVariable Long bookingId,
                              Authentication authentication,
                              @RequestBody(required = false) DecisionRequest request) {
        bookingService.reject(bookingId, authentication.getName(), request);
        return ApiResponse.ok("Booking rejected", null);
    }

    @PostMapping("/{bookingId}/withdraw")
    public ApiResponse withdraw(@PathVariable Long bookingId, Authentication authentication) {
        bookingService.withdraw(bookingId, authentication.getName());
        return ApiResponse.ok("Booking request withdrawn", null);
    }

    @PostMapping("/{bookingId}/cancel")
    public ApiResponse cancel(@PathVariable Long bookingId,
                              Authentication authentication,
                              @RequestBody(required = false) CancelRequest request) {
        bookingService.cancelBySeeker(bookingId, authentication.getName(), request);
        return ApiResponse.ok("Booking cancelled", null);
    }

    @PostMapping("/{bookingId}/cancel-by-staff")
    public ApiResponse cancelByStaff(@PathVariable Long bookingId,
                                     Authentication authentication,
                                     @RequestBody(required = false) CancelRequest request) {
        bookingService.cancelByStaff(bookingId, authentication.getName(), request);
        return ApiResponse.ok("Booking cancelled", null);
    }

    @PostMapping("/{bookingId}/complete")
    public ApiResponse complete(@PathVariable Long bookingId, Authentication authentication) {
        bookingService.complete(bookingId, authentication.getName());
        return ApiResponse.ok("Booking completed", null);
    }

    @PostMapping("/{bookingId}/no-show")
    public ApiResponse markNoShow(@PathVariable Long bookingId, Authentication authentication) {
        bookingService.markNoShow(bookingId, authentication.getName());
        return ApiResponse.ok("Booking marked as no-show", null);
    }
}
