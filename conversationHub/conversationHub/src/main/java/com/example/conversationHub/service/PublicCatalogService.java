package com.example.conversationHub.service;

import com.example.conversationHub.domain.AvailabilitySlot;
import com.example.conversationHub.domain.Institution;
import com.example.conversationHub.domain.Provider;
import com.example.conversationHub.domain.ServiceEntity;
import com.example.conversationHub.domain.enums.InstitutionStatus;
import com.example.conversationHub.domain.enums.VerificationStatus;
import com.example.conversationHub.repository.AvailabilitySlotRepository;
import com.example.conversationHub.repository.InstitutionRepository;
import com.example.conversationHub.repository.ProviderRepository;
import com.example.conversationHub.repository.ServiceRepository;
import com.example.conversationHub.web.dto.InstitutionView;
import com.example.conversationHub.web.dto.ProviderView;
import com.example.conversationHub.web.dto.ServiceView;
import com.example.conversationHub.web.dto.SlotView;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class PublicCatalogService {

    private final ServiceRepository serviceRepository;
    private final ProviderRepository providerRepository;
    private final AvailabilitySlotRepository slotRepository;
    private final InstitutionRepository institutionRepository;

    public PublicCatalogService(ServiceRepository serviceRepository,
                                ProviderRepository providerRepository,
                                AvailabilitySlotRepository slotRepository,
                                InstitutionRepository institutionRepository) {
        this.serviceRepository = serviceRepository;
        this.providerRepository = providerRepository;
        this.slotRepository = slotRepository;
        this.institutionRepository = institutionRepository;
    }

    @Transactional(readOnly = true)
    public List<ServiceView> listServices() {
        return serviceRepository.findAll().stream()
                .filter(ServiceEntity::isActive)
                .map(this::toServiceView)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProviderView> listVerifiedProviders() {
        return providerRepository.findByVerificationStatus(VerificationStatus.VERIFIED).stream()
                .filter(Provider::isActive)
                .map(this::toProviderView)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<InstitutionView> listApprovedInstitutions() {
        return institutionRepository.findByStatus(InstitutionStatus.APPROVED).stream()
                .map(this::toInstitutionView)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SlotView> listAvailableSlots(Long providerId) {
        Provider provider = providerRepository.findById(providerId)
                .orElseThrow(() -> new IllegalArgumentException("Provider not found"));
        if (!provider.isActive() || provider.getVerificationStatus() != VerificationStatus.VERIFIED) {
            throw new IllegalArgumentException("Provider is not available for bookings");
        }
        if (provider.getInstitution() != null
                && provider.getInstitution().getStatus() != InstitutionStatus.APPROVED) {
            throw new IllegalArgumentException("Provider institution is not approved");
        }

        LocalDateTime now = LocalDateTime.now();
        return slotRepository
                .findByProviderIdAndStartTimeGreaterThanEqualAndEndTimeLessThanEqual(
                        providerId, now, now.plusMonths(3))
                .stream()
                .filter(slot -> !slot.isBlocked())
                .filter(slot -> slot.getBookedCount() < slot.getCapacity())
                .map(this::toSlotView)
                .toList();
    }

    private InstitutionView toInstitutionView(Institution institution) {
        return new InstitutionView(
                institution.getId(),
                institution.getName(),
                institution.getType(),
                institution.getLocation(),
                institution.getStatus(),
                institution.getManager() == null ? null : institution.getManager().getName()
        );
    }

    private SlotView toSlotView(AvailabilitySlot slot) {
        return new SlotView(
                slot.getId(),
                slot.getStartTime(),
                slot.getEndTime(),
                slot.getCapacity(),
                slot.getCapacity() - slot.getBookedCount()
        );
    }

    private ServiceView toServiceView(ServiceEntity entity) {
        return new ServiceView(
                entity.getId(),
                entity.getCategory(),
                entity.getServiceType(),
                entity.getDurationMinutes(),
                entity.getChargeInformation()
        );
    }

    private ProviderView toProviderView(Provider provider) {
        return new ProviderView(
                provider.getId(),
                provider.getName(),
                provider.getSpecialisation(),
                provider.getServiceMode(),
                provider.getVerificationStatus(),
                provider.getResponseDeadlineHours(),
                provider.getCancellationRule(),
                provider.getService() == null ? null : provider.getService().getServiceType()
        );
    }
}
