package com.example.conversationHub.service;

import com.example.conversationHub.domain.AppUser;
import com.example.conversationHub.domain.AvailabilitySlot;
import com.example.conversationHub.domain.Provider;
import com.example.conversationHub.domain.SlotRecurrence;
import com.example.conversationHub.repository.AvailabilitySlotRepository;
import com.example.conversationHub.repository.SlotRecurrenceRepository;
import com.example.conversationHub.web.dto.RecurrenceRequest;
import com.example.conversationHub.web.dto.RecurrenceView;
import com.example.conversationHub.web.dto.SlotResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@Transactional
public class SlotRecurrenceService {

    private final SlotRecurrenceRepository recurrenceRepository;
    private final AvailabilitySlotRepository slotRepository;
    private final AvailabilityService availabilityService;
    private final CurrentUserService currentUserService;
    private final AuditService auditService;

    public SlotRecurrenceService(SlotRecurrenceRepository recurrenceRepository,
                                 AvailabilitySlotRepository slotRepository,
                                 AvailabilityService availabilityService,
                                 CurrentUserService currentUserService,
                                 AuditService auditService) {
        this.recurrenceRepository = recurrenceRepository;
        this.slotRepository = slotRepository;
        this.availabilityService = availabilityService;
        this.currentUserService = currentUserService;
        this.auditService = auditService;
    }

    public Long createRecurrence(Long providerId, String username, RecurrenceRequest request) {
        AppUser actor = currentUserService.require(username);
        Provider provider = availabilityService.requireManagedProvider(actor, providerId);
        validate(request);

        SlotRecurrence recurrence = new SlotRecurrence();
        recurrence.setProvider(provider);
        recurrence.setDayOfWeek(request.dayOfWeek());
        recurrence.setStartTime(request.startTime());
        recurrence.setEndTime(request.endTime());
        recurrence.setCapacity(request.capacity());
        recurrence.setValidFrom(request.validFrom());
        recurrence.setValidUntil(request.validUntil());

        SlotRecurrence saved = recurrenceRepository.save(recurrence);
        auditService.record(actor, "SLOT_RECURRENCE_CREATED", "SLOT_RECURRENCE", saved.getId());
        return saved.getId();
    }

    @Transactional(readOnly = true)
    public List<RecurrenceView> listRecurrences(Long providerId, String username) {
        AppUser actor = currentUserService.require(username);
        availabilityService.requireManagedProvider(actor, providerId);
        return recurrenceRepository.findByProviderIdOrderByDayOfWeekAscStartTimeAsc(providerId).stream()
                .map(RecurrenceView::from)
                .toList();
    }

    public void deleteRecurrence(Long providerId, Long recurrenceId, String username) {
        AppUser actor = currentUserService.require(username);
        Provider provider = availabilityService.requireManagedProvider(actor, providerId);
        SlotRecurrence recurrence = recurrenceRepository.findById(recurrenceId)
                .orElseThrow(() -> new IllegalArgumentException("Recurrence not found"));
        if (!Objects.equals(recurrence.getProvider().getId(), provider.getId())) {
            throw new IllegalArgumentException("Recurrence does not belong to provider");
        }
        recurrenceRepository.delete(recurrence);
        auditService.record(actor, "SLOT_RECURRENCE_DELETED", "SLOT_RECURRENCE", recurrence.getId());
    }

    /**
     * Materialises every occurrence of the provider's weekly patterns in the
     * inclusive date range. Existing slots for the exact time are left intact.
     */
    public List<SlotResponse> generateSlots(Long providerId, String username,
                                            LocalDate from, LocalDate to) {
        AppUser actor = currentUserService.require(username);
        Provider provider = availabilityService.requireManagedProvider(actor, providerId);
        if (from == null || to == null || to.isBefore(from)) {
            throw new IllegalArgumentException("Valid from and to dates are required");
        }

        List<SlotRecurrence> recurrences = recurrenceRepository.findByProviderIdForUpdate(providerId);
        List<AvailabilitySlot> generated = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
            int dayOfWeek = date.getDayOfWeek().getValue();
            for (SlotRecurrence recurrence : recurrences) {
                if (!Objects.equals(recurrence.getDayOfWeek(), dayOfWeek)
                        || date.isBefore(recurrence.getValidFrom())
                        || date.isAfter(recurrence.getValidUntil())) {
                    continue;
                }
                LocalDateTime start = date.atTime(recurrence.getStartTime());
                LocalDateTime end = date.atTime(recurrence.getEndTime());
                if (!end.isAfter(start) || !start.isAfter(now)
                        || slotRepository.existsByProviderIdAndStartTimeAndEndTime(
                                providerId, start, end)) {
                    continue;
                }

                AvailabilitySlot slot = new AvailabilitySlot();
                slot.setProvider(provider);
                slot.setStartTime(start);
                slot.setEndTime(end);
                slot.setCapacity(recurrence.getCapacity());
                slot.setBookedCount(0);
                slot.setBlocked(false);

                AvailabilitySlot saved = slotRepository.save(slot);
                generated.add(saved);
                auditService.record(actor, "RECURRENCE_SLOTS_GENERATED",
                        "AVAILABILITY_SLOT", saved.getId());
            }
        }
        return generated.stream().map(this::toView).toList();
    }

    private void validate(RecurrenceRequest request) {
        if (request.dayOfWeek() == null || request.dayOfWeek() < 1 || request.dayOfWeek() > 7) {
            throw new IllegalArgumentException("Day of week must be between 1 and 7");
        }
        if (request.startTime() == null || request.endTime() == null
                || !request.endTime().isAfter(request.startTime())) {
            throw new IllegalArgumentException("End time must be after start time");
        }
        if (request.capacity() == null || request.capacity() < 1) {
            throw new IllegalArgumentException("Capacity must be at least 1");
        }
        if (request.validFrom() == null || request.validUntil() == null
                || request.validUntil().isBefore(request.validFrom())) {
            throw new IllegalArgumentException("Valid until must not be before valid from");
        }
    }

    private SlotResponse toView(AvailabilitySlot slot) {
        return new SlotResponse(
                slot.getId(),
                slot.getProvider().getId(),
                slot.getStartTime(),
                slot.getEndTime(),
                slot.getCapacity(),
                slot.getBookedCount(),
                slot.isBlocked()
        );
    }
}
