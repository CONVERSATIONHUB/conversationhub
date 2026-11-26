package com.example.conversationHub.service;

import com.example.conversationHub.domain.AppUser;
import com.example.conversationHub.domain.AvailabilitySlot;
import com.example.conversationHub.domain.Institution;
import com.example.conversationHub.domain.Provider;
import com.example.conversationHub.domain.ServiceEntity;
import com.example.conversationHub.domain.enums.AccountType;
import com.example.conversationHub.domain.enums.InstitutionStatus;
import com.example.conversationHub.domain.enums.ServiceCategory;
import com.example.conversationHub.domain.enums.ServiceMode;
import com.example.conversationHub.domain.enums.VerificationStatus;
import com.example.conversationHub.repository.AvailabilitySlotRepository;
import com.example.conversationHub.repository.InstitutionRepository;
import com.example.conversationHub.repository.ProviderRepository;
import com.example.conversationHub.repository.ServiceRepository;
import com.example.conversationHub.web.dto.InstitutionView;
import com.example.conversationHub.web.dto.ProviderDetailView;
import com.example.conversationHub.web.dto.ProviderFilter;
import com.example.conversationHub.web.dto.ProviderView;
import com.example.conversationHub.web.dto.ServiceRequest;
import com.example.conversationHub.web.dto.ServiceView;
import com.example.conversationHub.web.dto.SlotView;
import com.example.conversationHub.web.exception.ForbiddenException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class PublicCatalogService {

    private static final long INVALID_ID = -1L;

    private final ServiceRepository serviceRepository;
    private final ProviderRepository providerRepository;
    private final AvailabilitySlotRepository slotRepository;
    private final InstitutionRepository institutionRepository;
    private final CurrentUserService currentUserService;
    private final AuditService auditService;

    public PublicCatalogService(ServiceRepository serviceRepository,
                                ProviderRepository providerRepository,
                                AvailabilitySlotRepository slotRepository,
                                InstitutionRepository institutionRepository,
                                CurrentUserService currentUserService,
                                AuditService auditService) {
        this.serviceRepository = serviceRepository;
        this.providerRepository = providerRepository;
        this.slotRepository = slotRepository;
        this.institutionRepository = institutionRepository;
        this.currentUserService = currentUserService;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<ServiceView> listServices() {
        return listServices(ProviderFilter.empty());
    }

    @Transactional(readOnly = true)
    public List<ServiceView> listServices(ProviderFilter filter) {
        ProviderFilter f = filter == null ? ProviderFilter.empty() : filter;
        ServiceCategory category = parseCategory(f.category());
        if (StringUtils.hasText(f.category()) && category == null) {
            return List.of();
        }
        Long institutionId = parseId(f.institutionId());
        Long providerId = parseId(f.providerId());
        if (institutionId != null && institutionId == INVALID_ID
                || providerId != null && providerId == INVALID_ID) {
            return List.of();
        }

        boolean providerFiltersPresent = institutionId != null || providerId != null
                || StringUtils.hasText(f.location());
        Set<Long> visibleServiceIds = null;
        if (providerFiltersPresent) {
            visibleServiceIds = publiclyVisibleProviders().stream()
                    .filter(provider -> institutionId == null
                            || provider.getInstitution() != null
                            && Objects.equals(provider.getInstitution().getId(), institutionId))
                    .filter(provider -> providerId == null
                            || Objects.equals(provider.getId(), providerId))
                    .filter(provider -> !StringUtils.hasText(f.location())
                            || matchesLocation(provider, f.location()))
                    .map(provider -> provider.getService() == null ? null : provider.getService().getId())
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
        }

        final Set<Long> allowedIds = visibleServiceIds;
        return serviceRepository.findByActiveTrue().stream()
                .filter(service -> category == null || service.getCategory() == category)
                .filter(service -> !StringUtils.hasText(f.serviceType())
                        || containsIgnoreCase(service.getServiceType(), f.serviceType()))
                .filter(service -> allowedIds == null || allowedIds.contains(service.getId()))
                .map(this::toServiceView)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProviderView> listVerifiedProviders() {
        return listVerifiedProviders(ProviderFilter.empty());
    }

    @Transactional(readOnly = true)
    public List<ProviderView> listVerifiedProviders(ProviderFilter filter) {
        ProviderFilter f = filter == null ? ProviderFilter.empty() : filter;
        ServiceCategory category = parseCategory(f.category());
        if (StringUtils.hasText(f.category()) && category == null) {
            return List.of();
        }
        Long institutionId = parseId(f.institutionId());
        Long providerId = parseId(f.providerId());
        if (institutionId != null && institutionId == INVALID_ID
                || providerId != null && providerId == INVALID_ID) {
            return List.of();
        }

        return publiclyVisibleProviders().stream()
                .filter(provider -> category == null || provider.getService() != null
                        && provider.getService().getCategory() == category)
                .filter(provider -> institutionId == null
                        || provider.getInstitution() != null
                        && Objects.equals(provider.getInstitution().getId(), institutionId))
                .filter(provider -> providerId == null
                        || Objects.equals(provider.getId(), providerId))
                .filter(provider -> !StringUtils.hasText(f.serviceType())
                        || provider.getService() != null
                        && containsIgnoreCase(provider.getService().getServiceType(), f.serviceType()))
                .filter(provider -> !StringUtils.hasText(f.location())
                        || matchesLocation(provider, f.location()))
                .map(this::toProviderView)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProviderDetailView getProviderDetail(Long providerId) {
        Provider provider = providerRepository.findById(providerId)
                .orElseThrow(() -> new IllegalArgumentException("Provider not found"));
        if (!isPubliclyVisible(provider)) {
            throw new IllegalArgumentException("Provider is not available");
        }
        int availableSlots = countAvailableSlots(provider);
        ServiceEntity service = provider.getService();
        return new ProviderDetailView(
                provider.getId(),
                provider.getName(),
                provider.getInstitution() == null ? null : provider.getInstitution().getName(),
                service == null ? null : service.getServiceType(),
                provider.getSpecialisation(),
                provider.getVerificationStatus(),
                modeLabel(provider.getServiceMode()),
                provider.getLocation(),
                service == null ? null : service.getChargeInformation(),
                provider.getResponseDeadlineHours(),
                provider.getCancellationRule(),
                provider.getMinLeadTimeHours(),
                provider.getCancellationCutoffHours(),
                provider.getNoShowRule(),
                provider.getNoShowRestrictionHours(),
                availableSlots,
                availableSlots > 0
        );
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
        if (!isPubliclyVisible(provider)) {
            throw new IllegalArgumentException("Provider is not available for bookings");
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

    /* ---------- Service catalogue administration (NFR-9.3 / BR-15) ---------- */

    @Transactional(readOnly = true)
    public List<ServiceView> listAllServices() {
        return serviceRepository.findAll().stream()
                .map(this::toServiceView)
                .toList();
    }

    public ServiceView createService(ServiceRequest request, String username) {
        requireAdmin(username);
        ServiceCategory category = parseCategory(request.category() == null ? null : request.category().name());
        if (category == null) {
            throw new IllegalArgumentException("Service category is required");
        }
        ServiceEntity entity = new ServiceEntity();
        entity.setCategory(category);
        entity.setServiceType(request.serviceType());
        entity.setDurationMinutes(request.durationMinutes() == null ? 30 : request.durationMinutes());
        entity.setChargeInformation(request.chargeInformation());
        entity.setActive(request.active() == null || request.active());
        ServiceEntity saved = serviceRepository.save(entity);
        auditService.record(currentUserService.require(username), "SERVICE_CREATED", "SERVICE", saved.getId());
        return toServiceView(saved);
    }

    public ServiceView updateService(Long id, ServiceRequest request, String username) {
        AppUser actor = requireAdmin(username);
        Long serviceId = id == null ? request.id() : id;
        if (serviceId == null) {
            throw new IllegalArgumentException("Service id is required");
        }
        ServiceEntity entity = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new IllegalArgumentException("Service not found"));
        if (request.category() != null) {
            entity.setCategory(request.category());
        }
        if (StringUtils.hasText(request.serviceType())) {
            entity.setServiceType(request.serviceType());
        }
        if (request.durationMinutes() != null) {
            entity.setDurationMinutes(request.durationMinutes());
        }
        if (request.chargeInformation() != null) {
            entity.setChargeInformation(request.chargeInformation());
        }
        if (request.active() != null) {
            entity.setActive(request.active());
        }
        ServiceEntity saved = serviceRepository.save(entity);
        auditService.record(actor, "SERVICE_UPDATED", "SERVICE", saved.getId());
        return toServiceView(saved);
    }

    /* ---------- helpers ---------- */

    private List<Provider> publiclyVisibleProviders() {
        return providerRepository
                .findByPublishedTrueAndActiveTrueAndVerificationStatus(VerificationStatus.VERIFIED)
                .stream()
                .filter(this::isPubliclyVisible)
                .toList();
    }

    private boolean isPubliclyVisible(Provider provider) {
        return provider.isActive()
                && provider.isPublished()
                && provider.getVerificationStatus() == VerificationStatus.VERIFIED
                && (provider.getInstitution() == null
                || provider.getInstitution().getStatus() == InstitutionStatus.APPROVED);
    }

    private boolean matchesLocation(Provider provider, String location) {
        if (containsIgnoreCase(provider.getLocation(), location)) {
            return true;
        }
        return provider.getInstitution() != null
                && containsIgnoreCase(provider.getInstitution().getLocation(), location);
    }

    private boolean containsIgnoreCase(String value, String needle) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(needle.toLowerCase(Locale.ROOT));
    }

    private ServiceCategory parseCategory(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return ServiceCategory.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private Long parseId(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return Long.valueOf(value.trim());
        } catch (NumberFormatException ex) {
            return INVALID_ID;
        }
    }

    private String modeLabel(ServiceMode mode) {
        return mode == null ? null : mode.getLabel();
    }

    private int countAvailableSlots(Provider provider) {
        LocalDateTime now = LocalDateTime.now();
        return (int) slotRepository
                .findByProviderIdAndStartTimeGreaterThanEqualAndEndTimeLessThanEqual(
                        provider.getId(), now, now.plusMonths(3))
                .stream()
                .filter(slot -> !slot.isBlocked())
                .filter(slot -> slot.getBookedCount() < slot.getCapacity())
                .count();
    }

    private AppUser requireAdmin(String username) {
        if (username == null || username.isBlank()) {
            throw new ForbiddenException("Main administrator access is required");
        }
        AppUser user = currentUserService.require(username);
        if (user.getAccountType() != AccountType.MAIN_ADMIN) {
            throw new ForbiddenException("Main administrator access is required");
        }
        return user;
    }

    private InstitutionView toInstitutionView(Institution institution) {
        return new InstitutionView(
                institution.getId(),
                institution.getName(),
                institution.getType(),
                institution.getLocation(),
                institution.getStatus(),
                institution.getManager() == null ? null : institution.getManager().getName(),
                institution.getContactPhone(),
                institution.getContactEmail(),
                institution.getContactAddress()
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
                entity.getChargeInformation(),
                entity.isActive()
        );
    }

    private ProviderView toProviderView(Provider provider) {
        int availableSlots = countAvailableSlots(provider);
        return new ProviderView(
                provider.getId(),
                provider.getName(),
                provider.getSpecialisation(),
                provider.getServiceMode(),
                provider.getVerificationStatus(),
                provider.getResponseDeadlineHours(),
                provider.getCancellationRule(),
                provider.getService() == null ? null : provider.getService().getServiceType(),
                provider.getInstitution() == null ? null : provider.getInstitution().getName(),
                provider.getLocation(),
                provider.getService() == null ? null : provider.getService().getChargeInformation(),
                modeLabel(provider.getServiceMode()),
                availableSlots,
                availableSlots > 0
        );
    }
}
