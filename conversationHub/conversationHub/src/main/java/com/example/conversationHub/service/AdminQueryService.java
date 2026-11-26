package com.example.conversationHub.service;

import com.example.conversationHub.domain.AuditLog;
import com.example.conversationHub.domain.Booking;
import com.example.conversationHub.repository.AuditLogRepository;
import com.example.conversationHub.repository.BookingRepository;
import com.example.conversationHub.web.dto.AuditLogView;
import com.example.conversationHub.web.dto.BookingView;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class AdminQueryService {

    private final BookingRepository bookingRepository;
    private final AuditLogRepository auditLogRepository;

    public AdminQueryService(BookingRepository bookingRepository,
                             AuditLogRepository auditLogRepository) {
        this.bookingRepository = bookingRepository;
        this.auditLogRepository = auditLogRepository;
    }

    public List<BookingView> allBookings() {
        return bookingRepository.findAll().stream().map(this::toBookingView).toList();
    }

    public List<AuditLogView> recentAuditLogs() {
        return auditLogRepository.findAll().stream()
                .sorted((a, b) -> b.getPerformedAt().compareTo(a.getPerformedAt()))
                .limit(200)
                .map(this::toAuditView)
                .toList();
    }

    private BookingView toBookingView(Booking booking) {
        return new BookingView(
                booking.getId(),
                booking.getStatus(),
                booking.getProvider().getName(),
                booking.getService() == null ? null : booking.getService().getServiceType(),
                booking.getSlot().getStartTime(),
                booking.getSlot().getEndTime(),
                booking.getAttendeeName(),
                booking.getRelationship(),
                booking.getSubmittedAt(),
                booking.getExpiresAt(),
                booking.getDecidedAt(),
                booking.getDecisionReason(),
                booking.getCancellationRule(),
                booking.getResponseDeadlineHours()
        );
    }

    private AuditLogView toAuditView(AuditLog log) {
        return new AuditLogView(
                log.getId(),
                log.getUser() == null ? null : log.getUser().getId(),
                log.getAction(),
                log.getEntityType(),
                log.getEntityId(),
                log.getPerformedAt()
        );
    }
}
