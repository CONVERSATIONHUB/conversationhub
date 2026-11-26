package com.example.conversationHub.service;

import com.example.conversationHub.domain.AppUser;
import com.example.conversationHub.domain.AuditLog;
import com.example.conversationHub.domain.Booking;
import com.example.conversationHub.domain.enums.AccountType;
import com.example.conversationHub.repository.AuditLogRepository;
import com.example.conversationHub.repository.BookingRepository;
import com.example.conversationHub.web.dto.AuditLogView;
import com.example.conversationHub.web.dto.BookingView;
import com.example.conversationHub.web.exception.ForbiddenException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class AdminQueryService {

    private final BookingRepository bookingRepository;
    private final AuditLogRepository auditLogRepository;
    private final CurrentUserService currentUserService;

    public AdminQueryService(BookingRepository bookingRepository,
                             AuditLogRepository auditLogRepository,
                             CurrentUserService currentUserService) {
        this.bookingRepository = bookingRepository;
        this.auditLogRepository = auditLogRepository;
        this.currentUserService = currentUserService;
    }

    public List<BookingView> allBookings() {
        return bookingRepository.findAll().stream().map(this::toBookingView).toList();
    }

    /** Role-checked variant used by the REST controller. */
    public List<BookingView> allBookings(String username) {
        requireAdmin(username);
        return allBookings();
    }

    public List<AuditLogView> recentAuditLogs() {
        return auditLogRepository.findTop200ByOrderByPerformedAtDesc().stream()
                .map(this::toAuditView)
                .toList();
    }

    /** Role-checked variant used by the REST controller. */
    public List<AuditLogView> recentAuditLogs(String username) {
        requireAdmin(username);
        return recentAuditLogs();
    }

    private void requireAdmin(String username) {
        AppUser actor = currentUserService.require(username);
        if (actor.getAccountType() != AccountType.MAIN_ADMIN) {
            throw new ForbiddenException("Main administrator access is required");
        }
    }

    private BookingView toBookingView(Booking booking) {
        return BookingView.from(booking);
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
