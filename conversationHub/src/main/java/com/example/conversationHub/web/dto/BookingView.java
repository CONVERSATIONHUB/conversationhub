package com.example.conversationHub.web.dto;

import com.example.conversationHub.domain.AppUser;
import com.example.conversationHub.domain.AvailabilitySlot;
import com.example.conversationHub.domain.Booking;
import com.example.conversationHub.domain.Provider;
import com.example.conversationHub.domain.ServiceEntity;
import com.example.conversationHub.domain.enums.BookingStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record BookingView(
        Long id,
        BookingStatus status,
        String providerName,
        String serviceType,
        LocalDateTime startTime,
        LocalDateTime endTime,
        String attendeeName,
        String relationship,
        LocalDateTime submittedAt,
        LocalDateTime expiresAt,
        LocalDateTime decidedAt,
        String decisionReason,
        String cancellationRule,
        Integer responseDeadlineHours,
        String seekerName,
        String seekerPhone,
        String seekerEmail,
        String message,
        String serviceCategory,
        boolean currentDay,
        String noShowRule,
        Integer minLeadTimeHours,
        Integer cancellationCutoffHours,
        String location,
        LocalDateTime cancelledAt,
        LocalDateTime expiredAt,
        LocalDateTime withdrawnAt,
        boolean bookingForOther
) {

    /** Kept for callers built against the original view shape. */
    public BookingView(Long id, BookingStatus status, String providerName, String serviceType,
                       LocalDateTime startTime, LocalDateTime endTime, String attendeeName,
                       String relationship, LocalDateTime submittedAt, LocalDateTime expiresAt,
                       LocalDateTime decidedAt, String decisionReason, String cancellationRule,
                       Integer responseDeadlineHours) {
        this(id, status, providerName, serviceType, startTime, endTime, attendeeName, relationship,
                submittedAt, expiresAt, decidedAt, decisionReason, cancellationRule,
                responseDeadlineHours, null, null, null, null, null, false, null, null, null,
                null, null, null, null, false);
    }

    public static BookingView from(Booking booking) {
        AppUser seeker = booking.getSeeker();
        Provider provider = booking.getProvider();
        ServiceEntity service = booking.getService();
        AvailabilitySlot slot = booking.getSlot();

        LocalDateTime start = slot == null ? null : slot.getStartTime();
        boolean currentDay = start != null && start.toLocalDate().equals(LocalDate.now());

        return new BookingView(
                booking.getId(),
                booking.getStatus(),
                provider == null ? null : provider.getName(),
                service == null ? null : service.getServiceType(),
                start,
                slot == null ? null : slot.getEndTime(),
                booking.getAttendeeName(),
                booking.getRelationship(),
                booking.getSubmittedAt(),
                booking.getExpiresAt(),
                booking.getDecidedAt(),
                booking.getDecisionReason(),
                booking.getCancellationRule(),
                booking.getResponseDeadlineHours(),
                seeker == null ? null : seeker.getName(),
                seeker == null ? null : seeker.getPhone(),
                seeker == null ? null : seeker.getEmail(),
                booking.getMessage(),
                service == null || service.getCategory() == null ? null : service.getCategory().name(),
                currentDay,
                provider == null || provider.getNoShowRule() == null ? null : provider.getNoShowRule(),
                provider == null || provider.getMinLeadTimeHours() == null ? 0 : provider.getMinLeadTimeHours(),
                provider == null || provider.getCancellationCutoffHours() == null ? 0 : provider.getCancellationCutoffHours(),
                provider == null ? null : provider.getLocation(),
                booking.getCancelledAt(),
                booking.getExpiredAt(),
                booking.getWithdrawnAt(),
                booking.isBookingForOther()
        );
    }
}
