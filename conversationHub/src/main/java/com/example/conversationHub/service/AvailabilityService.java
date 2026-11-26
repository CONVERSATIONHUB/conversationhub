package com.example.conversationHub.service;

import com.example.conversationHub.domain.AppUser;
import com.example.conversationHub.domain.AvailabilitySlot;
import com.example.conversationHub.domain.Provider;
import com.example.conversationHub.domain.enums.AccountType;
import com.example.conversationHub.repository.AvailabilitySlotRepository;
import com.example.conversationHub.repository.ProviderRepository;
import com.example.conversationHub.web.dto.SlotRequest;
import com.example.conversationHub.web.dto.SlotResponse;
import com.example.conversationHub.web.exception.ForbiddenException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
@Transactional
public class AvailabilityService {

    private final AvailabilitySlotRepository slotRepository;
    private final ProviderRepository providerRepository;
    private final CurrentUserService currentUserService;
    private final AuditService auditService;

    public AvailabilityService(AvailabilitySlotRepository slotRepository,
                               ProviderRepository providerRepository,
                               CurrentUserService currentUserService,
                               AuditService auditService) {
        this.slotRepository = slotRepository;
        this.providerRepository = providerRepository;
        this.currentUserService = currentUserService;
        this.auditService = auditService;
    }

    public Long createSlot(Long providerId, String username, SlotRequest request) {
        AppUser actor = currentUserService.require(username);
        Provider provider = requireManagedProvider(actor, providerId);
        validateSlotRequest(request);

        AvailabilitySlot slot = new AvailabilitySlot();
        apply(slot, request);
        slot.setProvider(provider);
        slot.setBookedCount(0);

        AvailabilitySlot saved = slotRepository.save(slot);
        auditService.record(actor, "SLOT_CREATED", "AVAILABILITY_SLOT", saved.getId());
        return saved.getId();
    }

    public void updateSlot(Long providerId, Long slotId, String username, SlotRequest request) {
        AppUser actor = currentUserService.require(username);
        Provider provider = requireManagedProvider(actor, providerId);
        AvailabilitySlot slot = requireSlotForUpdate(provider, slotId);
        validateSlotRequest(request);

        if (request.capacity() < slot.getBookedCount()) {
            throw new IllegalArgumentException("Capacity cannot be less than booked count");
        }

        apply(slot, request);
        slotRepository.save(slot);
        auditService.record(actor, "SLOT_UPDATED", "AVAILABILITY_SLOT", slot.getId());
    }

    public void setBlocked(Long providerId, Long slotId, String username, boolean blocked) {
        AppUser actor = currentUserService.require(username);
        Provider provider = requireManagedProvider(actor, providerId);
        AvailabilitySlot slot = requireSlotForUpdate(provider, slotId);
        slot.setBlocked(blocked);
        slotRepository.save(slot);
        auditService.record(actor, blocked ? "SLOT_BLOCKED" : "SLOT_UNBLOCKED",
                "AVAILABILITY_SLOT", slot.getId());
    }

    @Transactional(readOnly = true)
    public List<SlotResponse> listProviderSlots(Long providerId, String username) {
        AppUser actor = currentUserService.require(username);
        requireManagedProvider(actor, providerId);
        LocalDateTime from = LocalDateTime.now().minusDays(1);
        LocalDateTime to = LocalDateTime.now().plusMonths(6);
        return slotRepository.findByProviderIdAndStartTimeBetween(providerId, from, to).stream()
                .map(this::toView)
                .toList();
    }

    private void validateSlotRequest(SlotRequest request) {
        if (request.endTime() == null || request.startTime() == null
                || !request.endTime().isAfter(request.startTime())) {
            throw new IllegalArgumentException("End time must be after start time");
        }
        // A slot may never be moved into the past, whether created or updated (NFR-3.3).
        if (!request.startTime().isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("Slot start time must be in the future");
        }
    }

    private void apply(AvailabilitySlot slot, SlotRequest request) {
        slot.setStartTime(request.startTime());
        slot.setEndTime(request.endTime());
        slot.setCapacity(request.capacity());
        slot.setBlocked(Boolean.TRUE.equals(request.blocked()));
    }

    private AvailabilitySlot requireSlotForUpdate(Provider provider, Long slotId) {
        AvailabilitySlot slot = slotRepository.findByIdForUpdate(slotId)
                .orElseThrow(() -> new IllegalArgumentException("Slot not found"));
        if (!Objects.equals(slot.getProvider().getId(), provider.getId())) {
            throw new IllegalArgumentException("Slot does not belong to provider");
        }
        return slot;
    }

    /** Shared provider-authorisation rule for slots and weekly recurrence rules. */
    public Provider requireManagedProvider(AppUser actor, Long providerId) {
        Provider provider = providerRepository.findById(providerId)
                .orElseThrow(() -> new IllegalArgumentException("Provider not found"));

        if (actor.getAccountType() == AccountType.MAIN_ADMIN) {
            return provider;
        }

        if (actor.getAccountType() == AccountType.INDEPENDENT_PROVIDER
                || actor.getAccountType() == AccountType.INSTITUTIONAL_PROVIDER) {
            if (provider.getUser() != null && Objects.equals(provider.getUser().getId(), actor.getId())) {
                return provider;
            }
            throw new ForbiddenException("You can only manage your own availability");
        }

        if (actor.getAccountType() == AccountType.CENTER_MANAGER
                || actor.getAccountType() == AccountType.APPOINTMENT_OFFICER) {
            if (provider.getInstitution() != null && actor.getInstitution() != null
                    && Objects.equals(provider.getInstitution().getId(), actor.getInstitution().getId())) {
                return provider;
            }
            throw new ForbiddenException("Institution access is required");
        }

        throw new ForbiddenException("Not authorised to manage availability");
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
