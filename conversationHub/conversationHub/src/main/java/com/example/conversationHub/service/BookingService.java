package com.example.conversationHub.service;

import com.example.conversationHub.domain.AppUser;
import com.example.conversationHub.domain.AuditLog;
import com.example.conversationHub.domain.AvailabilitySlot;
import com.example.conversationHub.domain.Booking;
import com.example.conversationHub.domain.Provider;
import com.example.conversationHub.domain.enums.AccountType;
import com.example.conversationHub.domain.enums.BookingStatus;
import com.example.conversationHub.domain.enums.InstitutionStatus;
import com.example.conversationHub.domain.enums.VerificationStatus;
import com.example.conversationHub.repository.AppUserRepository;
import com.example.conversationHub.repository.AuditLogRepository;
import com.example.conversationHub.repository.AvailabilitySlotRepository;
import com.example.conversationHub.repository.BookingRepository;
import com.example.conversationHub.repository.ProviderRepository;
import com.example.conversationHub.web.dto.BookingView;
import com.example.conversationHub.web.dto.CancelRequest;
import com.example.conversationHub.web.dto.CreateBookingRequest;
import com.example.conversationHub.web.dto.DecisionRequest;
import com.example.conversationHub.web.exception.BookingExpiredException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
@Transactional(noRollbackFor = BookingExpiredException.class)
public class BookingService {

    private final BookingRepository bookingRepository;
    private final AppUserRepository userRepository;
    private final ProviderRepository providerRepository;
    private final AvailabilitySlotRepository slotRepository;
    private final AuditLogRepository auditLogRepository;
    private final NotificationService notificationService;

    public BookingService(BookingRepository bookingRepository,
                          AppUserRepository userRepository,
                          ProviderRepository providerRepository,
                          AvailabilitySlotRepository slotRepository,
                          AuditLogRepository auditLogRepository,
                          NotificationService notificationService) {
        this.bookingRepository = bookingRepository;
        this.userRepository = userRepository;
        this.providerRepository = providerRepository;
        this.slotRepository = slotRepository;
        this.auditLogRepository = auditLogRepository;
        this.notificationService = notificationService;
    }

    public Long requestBooking(String username, CreateBookingRequest request) {
        AppUser seeker = currentUser(username);
        if (seeker.getAccountType() != AccountType.SEEKER) {
            throw new IllegalArgumentException("Only seeker accounts can request appointments");
        }

        Provider provider = providerRepository.findById(request.providerId())
                .orElseThrow(() -> new IllegalArgumentException("Provider not found"));
        if (!provider.isActive() || provider.getVerificationStatus() != VerificationStatus.VERIFIED) {
            throw new IllegalArgumentException("Provider is not available for bookings");
        }
        if (provider.getInstitution() != null
                && provider.getInstitution().getStatus() != InstitutionStatus.APPROVED) {
            throw new IllegalArgumentException("Provider institution is not approved");
        }

        AvailabilitySlot slot = slotRepository.findByIdForUpdate(request.slotId())
                .orElseThrow(() -> new IllegalArgumentException("Slot not found"));
        if (!Objects.equals(slot.getProvider().getId(), provider.getId())) {
            throw new IllegalArgumentException("Slot does not belong to the selected provider");
        }
        if (slot.isBlocked()) {
            throw new IllegalArgumentException("Slot is blocked");
        }
        if (!slot.getStartTime().isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("Slot is in the past");
        }
        if (slot.getBookedCount() >= slot.getCapacity()) {
            throw new IllegalArgumentException("Slot is full");
        }

        // Hold the slot at request time so capacity cannot be exceeded by concurrent requests.
        slot.setBookedCount(slot.getBookedCount() + 1);
        slotRepository.save(slot);

        Booking booking = new Booking();
        booking.setSeeker(seeker);
        booking.setProvider(provider);
        booking.setService(provider.getService());
        booking.setSlot(slot);
        booking.setStatus(BookingStatus.PENDING);
        booking.setBookingForOther(request.bookingForOther());
        booking.setAttendeeName(request.bookingForOther() ? request.attendeeName() : null);
        booking.setRelationship(request.bookingForOther() ? request.relationship() : null);
        booking.setMessage(request.message());
        booking.setResponseDeadlineHours(provider.getResponseDeadlineHours());
        booking.setCancellationRule(provider.getCancellationRule());
        booking.setExpiresAt(LocalDateTime.now().plusHours(provider.getResponseDeadlineHours()));

        Booking saved = bookingRepository.save(booking);
        notificationService.notify(seeker, saved, "booking.requested");
        notifyDeciders(provider, saved);
        audit(seeker, "BOOKING_REQUESTED", "BOOKING", saved.getId());
        return saved.getId();
    }

    public void approve(Long bookingId, String username, DecisionRequest decision) {
        Booking booking = bookingRepository.findByIdForUpdate(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found"));
        AppUser approver = requireDecider(username, booking);
        ensurePendingAndNotExpired(booking);

        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setDecidedAt(LocalDateTime.now());
        booking.setDecisionReason(decision == null ? null : decision.reason());
        bookingRepository.save(booking);

        notificationService.notify(booking.getSeeker(), booking, "booking.approved");
        audit(approver, "BOOKING_APPROVED", "BOOKING", booking.getId());
    }

    public void reject(Long bookingId, String username, DecisionRequest decision) {
        Booking booking = bookingRepository.findByIdForUpdate(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found"));
        AppUser approver = requireDecider(username, booking);
        ensurePendingAndNotExpired(booking);

        releaseSlot(booking);
        booking.setStatus(BookingStatus.REJECTED);
        booking.setDecidedAt(LocalDateTime.now());
        booking.setDecisionReason(decision == null ? null : decision.reason());
        bookingRepository.save(booking);

        notificationService.notify(booking.getSeeker(), booking, "booking.rejected");
        audit(approver, "BOOKING_REJECTED", "BOOKING", booking.getId());
    }

    public void cancelBySeeker(Long bookingId, String username, CancelRequest request) {
        AppUser seeker = currentUser(username);
        Booking booking = bookingRepository.findByIdForUpdate(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found"));
        if (!Objects.equals(booking.getSeeker().getId(), seeker.getId())) {
            throw new IllegalArgumentException("This booking does not belong to you");
        }
        cancel(booking, seeker, request == null ? null : request.reason());
    }

    public void cancelByStaff(Long bookingId, String username, CancelRequest request) {
        AppUser actor = currentUser(username);
        Booking booking = bookingRepository.findByIdForUpdate(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found"));
        if (!canDecide(actor, booking.getProvider())) {
            throw new IllegalArgumentException("Not authorised to cancel this booking");
        }
        cancel(booking, actor, request == null ? null : request.reason());
    }

    public void complete(Long bookingId, String username) {
        AppUser actor = currentUser(username);
        Booking booking = bookingRepository.findByIdForUpdate(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found"));
        if (!canDecide(actor, booking.getProvider())) {
            throw new IllegalArgumentException("Not authorised to complete this booking");
        }
        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new IllegalArgumentException("Only confirmed bookings can be completed");
        }
        if (booking.getSlot().getEndTime().isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("Appointment time has not passed");
        }

        booking.setStatus(BookingStatus.COMPLETED);
        booking.setDecidedAt(LocalDateTime.now());
        bookingRepository.save(booking);
        notificationService.notify(booking.getSeeker(), booking, "booking.completed");
        audit(actor, "BOOKING_COMPLETED", "BOOKING", booking.getId());
    }

    public void markNoShow(Long bookingId, String username) {
        AppUser actor = currentUser(username);
        Booking booking = bookingRepository.findByIdForUpdate(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found"));
        if (!canDecide(actor, booking.getProvider())) {
            throw new IllegalArgumentException("Not authorised to mark this booking");
        }
        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new IllegalArgumentException("Only confirmed bookings can be marked as no-show");
        }
        if (booking.getSlot().getEndTime().isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("Appointment time has not passed");
        }

        booking.setStatus(BookingStatus.NO_SHOW);
        booking.setDecidedAt(LocalDateTime.now());
        bookingRepository.save(booking);
        notificationService.notify(booking.getSeeker(), booking, "booking.no_show");
        audit(actor, "BOOKING_NO_SHOW", "BOOKING", booking.getId());
    }

    public int expirePendingBookings() {
        List<Booking> expired = bookingRepository.findByExpiresAtBeforeAndStatus(
                LocalDateTime.now(), BookingStatus.PENDING);
        for (Booking booking : expired) {
            expire(booking);
        }
        return expired.size();
    }

    @Transactional(readOnly = true)
    public List<BookingView> myBookings(String username) {
        AppUser seeker = currentUser(username);
        return bookingRepository.findBySeekerId(seeker.getId()).stream()
                .map(this::toView)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<BookingView> providerBookings(String username) {
        AppUser user = currentUser(username);
        Provider provider = providerRepository.findByUserId(user.getId())
                .orElseThrow(() -> new IllegalArgumentException("No provider profile for this account"));
        return bookingRepository.findByProviderId(provider.getId()).stream()
                .map(this::toView)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<BookingView> institutionBookings(String username) {
        AppUser user = currentUser(username);
        if (user.getAccountType() == AccountType.MAIN_ADMIN) {
            return bookingRepository.findAll().stream()
                    .map(this::toView)
                    .toList();
        }
        if ((user.getAccountType() != AccountType.APPOINTMENT_OFFICER
                && user.getAccountType() != AccountType.CENTER_MANAGER)
                || user.getInstitution() == null) {
            throw new IllegalArgumentException("Institution staff access is required");
        }
        return bookingRepository.findByProvider_Institution_Id(user.getInstitution().getId()).stream()
                .map(this::toView)
                .toList();
    }

    @Transactional(readOnly = true)
    public BookingView getBooking(Long bookingId, String username) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found"));
        AppUser user = currentUser(username);
        boolean owner = Objects.equals(booking.getSeeker().getId(), user.getId());
        boolean ownProvider = user.getProvider() != null
                && Objects.equals(user.getProvider().getId(), booking.getProvider().getId());
        if (!owner && !ownProvider && !canDecide(user, booking.getProvider())) {
            throw new IllegalArgumentException("Not authorised to view this booking");
        }
        return toView(booking);
    }

    private void cancel(Booking booking, AppUser actor, String reason) {
        if (booking.getStatus() != BookingStatus.PENDING
                && booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new IllegalArgumentException("Booking cannot be cancelled in its current status");
        }
        releaseSlot(booking);
        booking.setStatus(BookingStatus.CANCELLED);
        booking.setDecidedAt(LocalDateTime.now());
        booking.setDecisionReason(reason);
        bookingRepository.save(booking);
        notificationService.notify(booking.getSeeker(), booking, "booking.cancelled");
        audit(actor, "BOOKING_CANCELLED", "BOOKING", booking.getId());
    }

    private void expire(Booking booking) {
        if (booking.getStatus() != BookingStatus.PENDING) {
            return;
        }
        releaseSlot(booking);
        booking.setStatus(BookingStatus.EXPIRED);
        booking.setDecidedAt(LocalDateTime.now());
        booking.setDecisionReason("Response deadline passed");
        bookingRepository.save(booking);
        notificationService.notify(booking.getSeeker(), booking, "booking.expired");
        audit(null, "BOOKING_EXPIRED", "BOOKING", booking.getId());
    }

    private void ensurePendingAndNotExpired(Booking booking) {
        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new IllegalArgumentException("Booking is not pending");
        }
        if (booking.getExpiresAt().isBefore(LocalDateTime.now())) {
            expire(booking);
            throw new BookingExpiredException("Booking request has expired");
        }
    }

    private AppUser requireDecider(String username, Booking booking) {
        AppUser actor = currentUser(username);
        if (!canDecide(actor, booking.getProvider())) {
            throw new IllegalArgumentException("Not authorised to decide this booking");
        }
        return actor;
    }

    private void notifyDeciders(Provider provider, Booking booking) {
        if (provider.getInstitution() != null) {
            userRepository.findByInstitution_Id(provider.getInstitution().getId()).stream()
                    .filter(user -> user.getAccountType() == AccountType.APPOINTMENT_OFFICER
                            || user.getAccountType() == AccountType.CENTER_MANAGER)
                    .forEach(user -> notificationService.notify(user, booking, "booking.new_request"));
        } else if (provider.getUser() != null) {
            notificationService.notify(provider.getUser(), booking, "booking.new_request");
        }
        userRepository.findByAccountType(AccountType.MAIN_ADMIN)
                .forEach(user -> notificationService.notify(user, booking, "booking.new_request"));
    }

    private boolean canDecide(AppUser actor, Provider provider) {
        if (actor.getAccountType() == AccountType.MAIN_ADMIN) {
            return true;
        }
        if (actor.getAccountType() == AccountType.INDEPENDENT_PROVIDER) {
            return provider.getUser() != null
                    && Objects.equals(provider.getUser().getId(), actor.getId())
                    && provider.getInstitution() == null;
        }
        if (actor.getAccountType() == AccountType.APPOINTMENT_OFFICER
                || actor.getAccountType() == AccountType.CENTER_MANAGER) {
            return provider.getInstitution() != null
                    && actor.getInstitution() != null
                    && Objects.equals(provider.getInstitution().getId(), actor.getInstitution().getId());
        }
        return false;
    }

    private void releaseSlot(Booking booking) {
        AvailabilitySlot slot = slotRepository.findByIdForUpdate(booking.getSlot().getId())
                .orElse(null);
        if (slot != null && slot.getBookedCount() > 0) {
            slot.setBookedCount(slot.getBookedCount() - 1);
            slotRepository.save(slot);
        }
    }

    private AppUser currentUser(String username) {
        return userRepository.findByPhone(username)
                .or(() -> userRepository.findByEmail(username))
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
    }

    private void audit(AppUser actor, String action, String entityType, Long entityId) {
        AuditLog log = new AuditLog();
        log.setUser(actor);
        log.setAction(action);
        log.setEntityType(entityType);
        log.setEntityId(entityId);
        auditLogRepository.save(log);
    }

    private BookingView toView(Booking booking) {
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
}
