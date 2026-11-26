package com.example.conversationHub.service;

import com.example.conversationHub.domain.AppUser;
import com.example.conversationHub.domain.AuditLog;
import com.example.conversationHub.domain.AvailabilitySlot;
import com.example.conversationHub.domain.Booking;
import com.example.conversationHub.domain.Provider;
import com.example.conversationHub.domain.RescheduleRequest;
import com.example.conversationHub.domain.enums.AccountType;
import com.example.conversationHub.domain.enums.BookingStatus;
import com.example.conversationHub.domain.enums.InstitutionStatus;
import com.example.conversationHub.domain.enums.VerificationStatus;
import com.example.conversationHub.repository.AppUserRepository;
import com.example.conversationHub.repository.AuditLogRepository;
import com.example.conversationHub.repository.AvailabilitySlotRepository;
import com.example.conversationHub.repository.BookingRepository;
import com.example.conversationHub.repository.ProviderRepository;
import com.example.conversationHub.repository.RescheduleRequestRepository;
import com.example.conversationHub.web.dto.BookingView;
import com.example.conversationHub.web.dto.CancelRequest;
import com.example.conversationHub.web.dto.CreateBookingRequest;
import com.example.conversationHub.web.dto.DecisionRequest;
import com.example.conversationHub.web.dto.RescheduleRequestDto;
import com.example.conversationHub.web.exception.BookingExpiredException;
import com.example.conversationHub.web.exception.ForbiddenException;
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
    private final RescheduleRequestRepository rescheduleRequestRepository;

    public BookingService(BookingRepository bookingRepository,
                          AppUserRepository userRepository,
                          ProviderRepository providerRepository,
                          AvailabilitySlotRepository slotRepository,
                          AuditLogRepository auditLogRepository,
                          NotificationService notificationService,
                          RescheduleRequestRepository rescheduleRequestRepository) {
        this.bookingRepository = bookingRepository;
        this.userRepository = userRepository;
        this.providerRepository = providerRepository;
        this.slotRepository = slotRepository;
        this.auditLogRepository = auditLogRepository;
        this.notificationService = notificationService;
        this.rescheduleRequestRepository = rescheduleRequestRepository;
    }

    public Long requestBooking(String username, CreateBookingRequest request) {
        AppUser seeker = currentUser(username);
        if (seeker.getAccountType() != AccountType.SEEKER) {
            throw new IllegalArgumentException("Only seeker accounts can request appointments");
        }
        if (!seeker.isActive()) {
            throw new IllegalArgumentException("This account is not active");
        }
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime restrictedUntil = seeker.getBookingRestrictedUntil();
        if (restrictedUntil != null && restrictedUntil.isAfter(now)) {
            throw new IllegalArgumentException(
                    "Booking is temporarily restricted until " + restrictedUntil);
        }

        Provider provider = providerRepository.findById(request.providerId())
                .orElseThrow(() -> new IllegalArgumentException("Provider not found"));
        if (!provider.isActive() || !provider.isPublished()
                || provider.getVerificationStatus() != VerificationStatus.VERIFIED) {
            throw new IllegalArgumentException("Provider is not available for bookings");
        }
        if (provider.getInstitution() != null
                && provider.getInstitution().getStatus() != InstitutionStatus.APPROVED) {
            throw new IllegalArgumentException("Provider institution is not approved");
        }

        AvailabilitySlot slot = slotRepository.findByIdForUpdate(request.slotId())
                .orElseThrow(() -> new IllegalArgumentException("Slot not found"));
        validateSlotForRequest(slot, provider, now);

        // Hold the slot at request time so capacity cannot be exceeded by concurrent requests.
        slot.setBookedCount(slot.getBookedCount() + 1);
        slotRepository.save(slot);

        Booking booking = new Booking();
        booking.setSeeker(seeker);
        booking.setProvider(provider);
        booking.setService(provider.getService());
        booking.setSlot(slot);
        booking.setStatus(BookingStatus.PENDING);
        booking.setCapacityHeld(true);
        booking.setBookingForOther(request.bookingForOther());
        booking.setAttendeeName(request.bookingForOther() ? request.attendeeName() : null);
        booking.setRelationship(request.bookingForOther() ? request.relationship() : null);
        booking.setMessage(request.message());
        booking.setResponseDeadlineHours(provider.getResponseDeadlineHours());
        booking.setCancellationRule(provider.getCancellationRule());
        booking.setExpiresAt(now.plusHours(provider.getResponseDeadlineHours()));

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
        ensurePendingAndNotExpired(booking, approver);
        validateSlotForApproval(booking);

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
        ensurePendingAndNotExpired(booking, approver);

        releaseSlot(booking);
        booking.setStatus(BookingStatus.REJECTED);
        booking.setDecidedAt(LocalDateTime.now());
        booking.setDecisionReason(decision == null ? null : decision.reason());
        bookingRepository.save(booking);

        notificationService.notify(booking.getSeeker(), booking, "booking.rejected");
        audit(approver, "BOOKING_REJECTED", "BOOKING", booking.getId());
    }

    public void withdraw(Long bookingId, String username) {
        AppUser seeker = currentUser(username);
        Booking booking = bookingRepository.findByIdForUpdate(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found"));
        if (!Objects.equals(booking.getSeeker().getId(), seeker.getId())) {
            throw new IllegalArgumentException("This booking does not belong to you");
        }
        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new IllegalArgumentException("Only pending requests can be withdrawn");
        }

        releaseSlot(booking);
        LocalDateTime now = LocalDateTime.now();
        booking.setStatus(BookingStatus.WITHDRAWN);
        booking.setWithdrawnAt(now);
        booking.setDecidedAt(now);
        bookingRepository.save(booking);

        notificationService.notify(seeker, booking, "booking.withdrawn");
        audit(seeker, "BOOKING_WITHDRAWN", "BOOKING", booking.getId());
    }

    public void cancelBySeeker(Long bookingId, String username, CancelRequest request) {
        AppUser seeker = currentUser(username);
        Booking booking = bookingRepository.findByIdForUpdate(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found"));
        if (!Objects.equals(booking.getSeeker().getId(), seeker.getId())) {
            throw new IllegalArgumentException("This booking does not belong to you");
        }
        cancel(booking, seeker, request == null ? null : request.reason(), false);
    }

    public void cancelByStaff(Long bookingId, String username, CancelRequest request) {
        AppUser actor = currentUser(username);
        Booking booking = bookingRepository.findByIdForUpdate(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found"));
        if (!canDecide(actor, booking.getProvider())) {
            throw new IllegalArgumentException("Not authorised to cancel this booking");
        }
        ensureNotSelf(actor, booking);
        cancel(booking, actor, request == null ? null : request.reason(), true);
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
        releasePendingReschedules(booking);
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
        releasePendingReschedules(booking);
        restrictSeeker(booking);
        notificationService.notify(booking.getSeeker(), booking, "booking.no_show");
        audit(actor, "BOOKING_NO_SHOW", "BOOKING", booking.getId());
    }

    public int expirePendingBookings() {
        LocalDateTime now = LocalDateTime.now();
        List<Booking> expiring = bookingRepository.findExpiringForUpdate(now, BookingStatus.PENDING);
        int expired = 0;
        for (Booking booking : expiring) {
            // The pessimistic lock is held; re-check the status before acting.
            if (booking.getStatus() == BookingStatus.PENDING
                    && !booking.getExpiresAt().isAfter(now)) {
                expire(booking, null);
                expired++;
            }
        }
        return expired;
    }

    public Long requestReschedule(Long bookingId, String username, RescheduleRequestDto request) {
        AppUser seeker = currentUser(username);
        Booking booking = bookingRepository.findByIdForUpdate(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found"));
        if (!Objects.equals(booking.getSeeker().getId(), seeker.getId())) {
            throw new IllegalArgumentException("This booking does not belong to you");
        }
        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new IllegalArgumentException("Only confirmed bookings can be rescheduled");
        }
        if (rescheduleRequestRepository.existsByBookingIdAndStatus(
                bookingId, RescheduleRequest.RescheduleStatus.PENDING)) {
            throw new IllegalArgumentException("A reschedule request is already pending");
        }
        if (request == null || request.newSlotId() == null) {
            throw new IllegalArgumentException("New slot is required");
        }

        AvailabilitySlot newSlot = slotRepository.findByIdForUpdate(request.newSlotId())
                .orElseThrow(() -> new IllegalArgumentException("Slot not found"));
        if (Objects.equals(newSlot.getId(), booking.getSlot().getId())) {
            throw new IllegalArgumentException("Choose a different slot");
        }
        validateSlotForRequest(newSlot, booking.getProvider(), LocalDateTime.now());

        newSlot.setBookedCount(newSlot.getBookedCount() + 1);
        slotRepository.save(newSlot);

        RescheduleRequest reschedule = new RescheduleRequest();
        reschedule.setBooking(booking);
        reschedule.setNewSlot(newSlot);
        reschedule.setStatus(RescheduleRequest.RescheduleStatus.PENDING);
        reschedule.setReason(request.reason());
        reschedule.setCapacityHeld(true);

        RescheduleRequest saved = rescheduleRequestRepository.save(reschedule);
        notificationService.notify(seeker, booking, "booking.reschedule_requested");
        notifyDeciders(booking.getProvider(), booking, "booking.reschedule_requested");
        audit(seeker, "BOOKING_RESCHEDULE_REQUESTED", "RESCHEDULE_REQUEST", saved.getId());
        return saved.getId();
    }

    @Transactional(readOnly = true)
    public List<RescheduleRequestDto> myReschedules(String username) {
        AppUser seeker = currentUser(username);
        return rescheduleRequestRepository
                .findByBooking_Seeker_IdOrderByRequestedAtDesc(seeker.getId()).stream()
                .map(RescheduleRequestDto::from)
                .toList();
    }

    public void approveReschedule(Long rescheduleId, String username, DecisionRequest decision) {
        RescheduleRequest reschedule = requirePendingReschedule(rescheduleId);
        Booking booking = bookingRepository.findByIdForUpdate(reschedule.getBooking().getId())
                .orElseThrow(() -> new IllegalArgumentException("Booking not found"));
        AppUser decider = requireDecider(username, booking);
        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new IllegalArgumentException("Booking is no longer confirmed");
        }
        // Same slot revalidation as a normal decision, applied to the proposed slot.
        validateSlotForApproval(reschedule.getNewSlot().getId(),
                reschedule.isCapacityHeld() ? 1 : 0, booking.getProvider().getId());

        // The old hold is released exactly once; the hold on the new slot stays with the booking.
        releaseSlot(booking);
        booking.setSlot(reschedule.getNewSlot());
        booking.setCapacityHeld(true);
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setDecidedAt(LocalDateTime.now());
        booking.setDecisionReason(decision == null ? null : decision.reason());
        bookingRepository.save(booking);

        reschedule.setStatus(RescheduleRequest.RescheduleStatus.APPROVED);
        reschedule.setCapacityHeld(false);
        reschedule.setDecidedAt(LocalDateTime.now());
        reschedule.setDecisionReason(decision == null ? null : decision.reason());
        rescheduleRequestRepository.save(reschedule);

        notificationService.notify(booking.getSeeker(), booking, "booking.rescheduled");
        audit(decider, "BOOKING_RESCHEDULE_APPROVED", "RESCHEDULE_REQUEST", reschedule.getId());
    }

    public void rejectReschedule(Long rescheduleId, String username, DecisionRequest decision) {
        RescheduleRequest reschedule = requirePendingReschedule(rescheduleId);
        Booking booking = bookingRepository.findByIdForUpdate(reschedule.getBooking().getId())
                .orElseThrow(() -> new IllegalArgumentException("Booking not found"));
        AppUser decider = requireDecider(username, booking);

        releaseSlot(reschedule);
        reschedule.setStatus(RescheduleRequest.RescheduleStatus.REJECTED);
        reschedule.setDecidedAt(LocalDateTime.now());
        reschedule.setDecisionReason(decision == null ? null : decision.reason());
        rescheduleRequestRepository.save(reschedule);

        notificationService.notify(booking.getSeeker(), booking, "booking.reschedule_rejected");
        audit(decider, "BOOKING_RESCHEDULE_REJECTED", "RESCHEDULE_REQUEST", reschedule.getId());
    }

    @Transactional(readOnly = true)
    public List<BookingView> myBookings(String username) {
        AppUser seeker = currentUser(username);
        return bookingRepository.findBySeekerId(seeker.getId()).stream()
                .map(BookingView::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<BookingView> providerBookings(String username) {
        AppUser user = currentUser(username);
        Provider provider = providerRepository.findByUserId(user.getId())
                .orElseThrow(() -> new IllegalArgumentException("No provider profile for this account"));
        return bookingRepository.findByProviderId(provider.getId()).stream()
                .map(BookingView::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<BookingView> institutionBookings(String username) {
        AppUser user = currentUser(username);
        if (user.getAccountType() == AccountType.MAIN_ADMIN) {
            return bookingRepository.findAll().stream()
                    .map(BookingView::from)
                    .toList();
        }
        if ((user.getAccountType() != AccountType.APPOINTMENT_OFFICER
                && user.getAccountType() != AccountType.CENTER_MANAGER)
                || user.getInstitution() == null) {
            throw new IllegalArgumentException("Institution staff access is required");
        }
        return bookingRepository.findByProvider_Institution_Id(user.getInstitution().getId()).stream()
                .map(BookingView::from)
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
        return BookingView.from(booking);
    }

    private void cancel(Booking booking, AppUser actor, String reason, boolean staff) {
        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new IllegalArgumentException("Only confirmed bookings can be cancelled");
        }
        boolean afterCutoff = isAfterCancellationCutoff(booking);
        if (staff && (reason == null || reason.isBlank())) {
            throw new IllegalArgumentException("A cancellation reason is required");
        }
        if (!staff && afterCutoff) {
            throw new IllegalArgumentException(
                    "Cancellation cutoff has passed; ask the provider or staff to cancel");
        }

        releaseSlot(booking);
        releasePendingReschedules(booking);
        LocalDateTime now = LocalDateTime.now();
        booking.setStatus(BookingStatus.CANCELLED);
        booking.setDecidedAt(now);
        booking.setCancelledAt(now);
        booking.setDecisionReason(reason);
        bookingRepository.save(booking);

        if (staff) {
            // A cancellation by staff after the cutoff has the same consequence as a no-show.
            if (afterCutoff) {
                restrictSeeker(booking);
            }
            notificationService.notify(booking.getSeeker(), booking, "booking.cancelled_by_provider");
        } else {
            notificationService.notify(booking.getSeeker(), booking, "booking.cancelled");
        }
        audit(actor, "BOOKING_CANCELLED", "BOOKING", booking.getId());
    }

    private void expire(Booking booking, AppUser actor) {
        if (booking.getStatus() != BookingStatus.PENDING) {
            return;
        }
        releaseSlot(booking);
        LocalDateTime now = LocalDateTime.now();
        booking.setStatus(BookingStatus.EXPIRED);
        booking.setDecidedAt(now);
        booking.setExpiredAt(now);
        booking.setDecisionReason("Response deadline passed");
        bookingRepository.save(booking);
        notificationService.notify(booking.getSeeker(), booking, "booking.expired");
        audit(actor, actor == null ? "SYSTEM_BOOKING_EXPIRED" : "BOOKING_EXPIRED",
                "BOOKING", booking.getId());
    }

    private void ensurePendingAndNotExpired(Booking booking, AppUser actor) {
        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new IllegalArgumentException("Booking is not pending");
        }
        if (!booking.getExpiresAt().isAfter(LocalDateTime.now())) {
            expire(booking, actor);
            throw new BookingExpiredException("Booking request has expired");
        }
    }

    private RescheduleRequest requirePendingReschedule(Long rescheduleId) {
        RescheduleRequest reschedule = rescheduleRequestRepository.findByIdForUpdate(rescheduleId)
                .orElseThrow(() -> new IllegalArgumentException("Reschedule request not found"));
        if (reschedule.getStatus() != RescheduleRequest.RescheduleStatus.PENDING) {
            throw new IllegalArgumentException("Reschedule request is not pending");
        }
        return reschedule;
    }

    private AppUser requireDecider(String username, Booking booking) {
        AppUser actor = currentUser(username);
        // FR-2.9 is checked before role-based rules so a requester can never decide
        // their own booking, even if their role has since been changed.
        ensureNotSelf(actor, booking);
        if (!canDecide(actor, booking.getProvider())) {
            throw new IllegalArgumentException("Not authorised to decide this booking");
        }
        return actor;
    }

    /**
     * FR-2.9: identity may be expressed through a changed role/account, but a
     * person must never decide a request that they submitted themselves.
     */
    private void ensureNotSelf(AppUser actor, Booking booking) {
        AppUser seeker = booking.getSeeker();
        if (Objects.equals(actor.getId(), seeker.getId())) {
            throw new ForbiddenException("You cannot decide your own booking request");
        }
        if (actor.getPhone() != null && actor.getPhone().equalsIgnoreCase(seeker.getPhone())) {
            throw new ForbiddenException("You cannot decide your own booking request");
        }
        if (actor.getEmail() != null && actor.getEmail().equalsIgnoreCase(seeker.getEmail())) {
            throw new ForbiddenException("You cannot decide your own booking request");
        }
    }

    private void notifyDeciders(Provider provider, Booking booking) {
        notifyDeciders(provider, booking, "booking.new_request");
    }

    private void notifyDeciders(Provider provider, Booking booking, String messageKey) {
        if (provider.getInstitution() != null) {
            userRepository.findByInstitution_Id(provider.getInstitution().getId()).stream()
                    .filter(user -> user.getAccountType() == AccountType.APPOINTMENT_OFFICER
                            || user.getAccountType() == AccountType.CENTER_MANAGER)
                    .forEach(user -> notificationService.notify(user, booking, messageKey));
        } else if (provider.getUser() != null) {
            notificationService.notify(provider.getUser(), booking, messageKey);
        }
        userRepository.findByAccountType(AccountType.MAIN_ADMIN)
                .forEach(user -> notificationService.notify(user, booking, messageKey));
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

    private void validateSlotForRequest(AvailabilitySlot slot, Provider provider, LocalDateTime now) {
        if (slot == null) {
            throw new IllegalArgumentException("Slot not found");
        }
        if (!Objects.equals(slot.getProvider().getId(), provider.getId())) {
            throw new IllegalArgumentException("Slot does not belong to the selected provider");
        }
        if (slot.isBlocked()) {
            throw new IllegalArgumentException("Slot is blocked");
        }
        if (!slot.getStartTime().isAfter(now)) {
            throw new IllegalArgumentException("Slot is in the past");
        }
        Integer leadTime = provider.getMinLeadTimeHours();
        if (leadTime != null && leadTime > 0 && slot.getStartTime().isBefore(now.plusHours(leadTime))) {
            throw new IllegalArgumentException("Slot does not meet the provider's minimum lead time");
        }
        if (slot.getBookedCount() >= slot.getCapacity()) {
            throw new IllegalArgumentException("Slot is full");
        }
    }

    /**
     * FR-7.11: re-read the slot under a lock at decision time. A pending request
     * already holds one unit of capacity, so that unit is not counted as a
     * conflicting booking here.
     */
    private void validateSlotForApproval(Booking booking) {
        validateSlotForApproval(booking.getSlot().getId(),
                booking.isCapacityHeld() ? 1 : 0, booking.getProvider().getId());
    }

    private void validateSlotForApproval(Long slotId, int ownHold, Long providerId) {
        AvailabilitySlot slot = slotRepository.findByIdForUpdate(slotId)
                .orElseThrow(() -> new IllegalArgumentException("Slot not found"));
        if (!Objects.equals(slot.getProvider().getId(), providerId)) {
            throw new IllegalArgumentException("Slot does not belong to the booking's provider");
        }
        if (slot.isBlocked()) {
            throw new IllegalArgumentException("Slot is no longer available");
        }
        if (!slot.getStartTime().isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("Slot is in the past");
        }
        int heldByOther = slot.getBookedCount() - ownHold;
        if (heldByOther >= slot.getCapacity()) {
            throw new IllegalArgumentException("Slot is no longer available");
        }
    }

    private boolean isAfterCancellationCutoff(Booking booking) {
        Integer cutoffHours = booking.getProvider().getCancellationCutoffHours();
        int hours = cutoffHours == null ? 0 : cutoffHours;
        LocalDateTime cutoff = booking.getSlot().getStartTime().minusHours(hours);
        return LocalDateTime.now().isAfter(cutoff);
    }

    private void restrictSeeker(Booking booking) {
        Integer restrictionHours = booking.getProvider().getNoShowRestrictionHours();
        int hours = restrictionHours == null ? 0 : restrictionHours;
        if (hours <= 0) {
            return;
        }
        AppUser seeker = booking.getSeeker();
        LocalDateTime until = LocalDateTime.now().plusHours(hours);
        LocalDateTime current = seeker.getBookingRestrictedUntil();
        if (current == null || current.isBefore(until)) {
            seeker.setBookingRestrictedUntil(until);
            userRepository.save(seeker);
        }
    }

    /** Releases the capacity held by a booking, at most once. */
    private void releaseSlot(Booking booking) {
        if (!booking.isCapacityHeld()) {
            return;
        }
        AvailabilitySlot slot = booking.getSlot();
        decrementSlot(slot == null ? null : slot.getId());
        booking.setCapacityHeld(false);
        bookingRepository.save(booking);
    }

    /** A closed booking cannot have an outstanding reschedule holding capacity. */
    private void releasePendingReschedules(Booking booking) {
        rescheduleRequestRepository.findByBookingIdOrderByRequestedAtDesc(booking.getId()).stream()
                .filter(reschedule -> reschedule.getStatus() == RescheduleRequest.RescheduleStatus.PENDING)
                .forEach(reschedule -> {
                    releaseSlot(reschedule);
                    reschedule.setStatus(RescheduleRequest.RescheduleStatus.REJECTED);
                    reschedule.setDecidedAt(LocalDateTime.now());
                    reschedule.setDecisionReason("Booking is no longer active");
                    rescheduleRequestRepository.save(reschedule);
                });
    }

    /** Releases the capacity held on a reschedule's new slot, at most once. */
    private void releaseSlot(RescheduleRequest reschedule) {
        if (!reschedule.isCapacityHeld()) {
            return;
        }
        AvailabilitySlot slot = reschedule.getNewSlot();
        decrementSlot(slot == null ? null : slot.getId());
        reschedule.setCapacityHeld(false);
        rescheduleRequestRepository.save(reschedule);
    }

    private void decrementSlot(Long slotId) {
        if (slotId == null) {
            return;
        }
        AvailabilitySlot slot = slotRepository.findByIdForUpdate(slotId).orElse(null);
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
}
