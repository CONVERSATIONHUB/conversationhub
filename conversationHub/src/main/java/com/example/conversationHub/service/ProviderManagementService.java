package com.example.conversationHub.service;

import com.example.conversationHub.domain.AppUser;
import com.example.conversationHub.domain.Institution;
import com.example.conversationHub.domain.Provider;
import com.example.conversationHub.domain.ServiceEntity;
import com.example.conversationHub.domain.VerificationRecord;
import com.example.conversationHub.domain.enums.AccountType;
import com.example.conversationHub.domain.enums.InstitutionStatus;
import com.example.conversationHub.domain.enums.ServiceCategory;
import com.example.conversationHub.domain.enums.ServiceMode;
import com.example.conversationHub.domain.enums.VerificationDecision;
import com.example.conversationHub.domain.enums.VerificationStatus;
import com.example.conversationHub.repository.AppUserRepository;
import com.example.conversationHub.repository.ProviderRepository;
import com.example.conversationHub.repository.ServiceRepository;
import com.example.conversationHub.repository.VerificationRecordRepository;
import com.example.conversationHub.web.dto.CreateInstitutionalProviderRequest;
import com.example.conversationHub.web.dto.ManagedProviderView;
import com.example.conversationHub.web.dto.ProviderUpdateRequest;
import com.example.conversationHub.web.dto.RegisterIndependentProviderRequest;
import com.example.conversationHub.web.dto.VerificationDetailView;
import com.example.conversationHub.web.exception.ForbiddenException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
@Transactional
public class ProviderManagementService {

    private final ProviderRepository providerRepository;
    private final ServiceRepository serviceRepository;
    private final AppUserRepository userRepository;
    private final VerificationRecordRepository verificationRecordRepository;
    private final AccountService accountService;
    private final AuditService auditService;
    private final CurrentUserService currentUserService;

    public ProviderManagementService(ProviderRepository providerRepository,
                                     ServiceRepository serviceRepository,
                                     AppUserRepository userRepository,
                                     VerificationRecordRepository verificationRecordRepository,
                                     AccountService accountService,
                                     AuditService auditService,
                                     CurrentUserService currentUserService) {
        this.providerRepository = providerRepository;
        this.serviceRepository = serviceRepository;
        this.userRepository = userRepository;
        this.verificationRecordRepository = verificationRecordRepository;
        this.accountService = accountService;
        this.auditService = auditService;
        this.currentUserService = currentUserService;
    }

    public Long registerIndependentProvider(RegisterIndependentProviderRequest request) {
        ServiceEntity service = requireService(request.serviceId());
        requireIndependentService(service);

        AppUser user = accountService.createUser(
                request.name(),
                request.phone(),
                request.email(),
                request.password(),
                AccountType.INDEPENDENT_PROVIDER,
                "INDEPENDENT_PROVIDER",
                request.languageCode(),
                null,
                false
        );

        Provider provider = buildProvider(
                user,
                null,
                service,
                request.name(),
                request.specialisation(),
                request.serviceMode(),
                request.responseDeadlineHours(),
                request.cancellationRule()
        );
        applyRegistrationDetails(provider, request.location(), request.identityDetails(),
                request.qualifications(), request.minLeadTimeHours(), request.cancellationCutoffHours(),
                request.noShowRule(), request.noShowRestrictionHours());
        Provider saved = linkAndSave(user, provider);
        recordVerification(saved, null, null, VerificationDecision.PENDING, request.documentReference());
        auditService.record(user, "INDEPENDENT_PROVIDER_REGISTERED", "PROVIDER", saved.getId());
        return saved.getId();
    }

    public Long createInstitutionalProvider(Long institutionId, String username,
                                            CreateInstitutionalProviderRequest request) {
        AppUser manager = requireCenterManager(username, institutionId);
        ServiceEntity service = requireService(request.serviceId());
        requireInstitutionalService(service);

        AppUser user = accountService.createUser(
                request.name(),
                request.phone(),
                request.email(),
                request.password(),
                AccountType.INSTITUTIONAL_PROVIDER,
                "INSTITUTIONAL_PROVIDER",
                request.languageCode(),
                manager.getInstitution(),
                false
        );

        Provider provider = buildProvider(
                user,
                manager.getInstitution(),
                service,
                request.name(),
                request.specialisation(),
                request.serviceMode(),
                request.responseDeadlineHours(),
                request.cancellationRule()
        );
        applyRegistrationDetails(provider, request.location(), request.identityDetails(),
                request.qualifications(), request.minLeadTimeHours(), request.cancellationCutoffHours(),
                request.noShowRule(), request.noShowRestrictionHours());
        Provider saved = linkAndSave(user, provider);
        recordVerification(saved, manager.getInstitution(), null, VerificationDecision.PENDING,
                request.documentReference());
        auditService.record(manager, "INSTITUTIONAL_PROVIDER_CREATED", "PROVIDER", saved.getId());
        return saved.getId();
    }

    @Transactional(readOnly = true)
    public List<ManagedProviderView> listPendingIndependentProviders(String username) {
        requireAdmin(username);
        // Re-verification requests keep their published VERIFIED status, so include those too.
        return providerRepository.findByInstitutionIsNull().stream()
                .filter(provider -> provider.getVerificationStatus() == VerificationStatus.PENDING
                        || provider.isPendingReverification())
                .map(this::toView)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ManagedProviderView> listInstitutionProviders(Long institutionId, String username) {
        requireCenterManager(username, institutionId);
        return providerRepository.findByInstitution_Id(institutionId).stream()
                .map(this::toView)
                .toList();
    }

    public void verifyIndependentProvider(Long providerId, String username) {
        decideIndependent(providerId, username, true, "INDEPENDENT_PROVIDER_VERIFIED");
    }

    public void rejectIndependentProvider(Long providerId, String username) {
        decideIndependent(providerId, username, false, "INDEPENDENT_PROVIDER_REJECTED");
    }

    public void verifyInstitutionalProvider(Long institutionId, Long providerId, String username) {
        decideInstitutional(institutionId, providerId, username, true, "INSTITUTIONAL_PROVIDER_VERIFIED");
    }

    public void rejectInstitutionalProvider(Long institutionId, Long providerId, String username) {
        decideInstitutional(institutionId, providerId, username, false, "INSTITUTIONAL_PROVIDER_REJECTED");
    }

    /** PUT /api/providers/me: changes to verified fields wait for re-verification. */
    public void updateOwnProvider(String username, ProviderUpdateRequest request) {
        AppUser actor = currentUserService.require(username);
        if (actor.getAccountType() != AccountType.INDEPENDENT_PROVIDER
                && actor.getAccountType() != AccountType.INSTITUTIONAL_PROVIDER) {
            throw new ForbiddenException("Only provider accounts can update a provider profile");
        }
        Provider provider = providerRepository.findByUserId(actor.getId())
                .orElseThrow(() -> new IllegalArgumentException("No provider profile for this account"));
        applyProfileUpdate(provider, request);
        auditService.record(actor, "PROVIDER_UPDATED", "PROVIDER", provider.getId());
    }

    /** PUT /api/institutions/{id}/providers/{providerId}: manager-scoped profile update. */
    public void updateInstitutionalProvider(Long institutionId, Long providerId, String username,
                                            ProviderUpdateRequest request) {
        AppUser manager = requireCenterManager(username, institutionId);
        Provider provider = requireInstitutionProvider(institutionId, providerId);
        applyProfileUpdate(provider, request);
        auditService.record(manager, "PROVIDER_UPDATED", "PROVIDER", provider.getId());
    }

    /**
     * FR-4.9: deactivation is soft - bookings and verification history are retained.
     * Allowed for the main administrator, the provider's own account, or its institution manager.
     */
    public void deactivateProvider(Long providerId, String username) {
        AppUser actor = currentUserService.require(username);
        Provider provider = providerRepository.findById(providerId)
                .orElseThrow(() -> new IllegalArgumentException("Provider not found"));

        boolean admin = actor.getAccountType() == AccountType.MAIN_ADMIN;
        boolean owner = actor.getProvider() != null
                && Objects.equals(actor.getProvider().getId(), provider.getId());
        boolean manager = actor.getAccountType() == AccountType.CENTER_MANAGER
                && provider.getInstitution() != null
                && actor.getInstitution() != null
                && Objects.equals(provider.getInstitution().getId(), actor.getInstitution().getId())
                && provider.getInstitution().getStatus() == InstitutionStatus.APPROVED;
        if (!admin && !owner && !manager) {
            throw new ForbiddenException("Not authorised to deactivate this provider");
        }

        provider.setActive(false);
        providerRepository.save(provider);
        auditService.record(actor, "PROVIDER_DEACTIVATED", "PROVIDER", provider.getId());
    }

    /** NFR-4.8: verification evidence is available to administrators only. */
    @Transactional(readOnly = true)
    public VerificationDetailView verificationDetail(Long providerId, String username) {
        requireAdmin(username);
        Provider provider = providerRepository.findById(providerId)
                .orElseThrow(() -> new IllegalArgumentException("Provider not found"));
        VerificationRecord record = verificationRecordRepository
                .findTopByProviderIdOrderByIdDesc(providerId)
                .orElseThrow(() -> new IllegalArgumentException("Verification record not found"));
        return new VerificationDetailView(
                record.getId(),
                provider.getId(),
                provider.getName(),
                provider.getInstitution() == null ? null : provider.getInstitution().getName(),
                record.getCredentialReference(),
                record.getIdentityDetails(),
                record.getQualifications(),
                record.getDocumentReference(),
                record.getDecision(),
                record.getDecidedBy() == null ? null : record.getDecidedBy().getName(),
                record.getDecidedAt(),
                record.getCreatedAt()
        );
    }

    private void decideIndependent(Long providerId, String username, boolean approved, String action) {
        AppUser admin = requireAdmin(username);
        Provider provider = providerRepository.findById(providerId)
                .orElseThrow(() -> new IllegalArgumentException("Provider not found"));
        if (provider.getInstitution() != null) {
            throw new IllegalArgumentException("Provider is not independent");
        }
        applyDecision(provider, approved, null, admin, action);
    }

    private void decideInstitutional(Long institutionId, Long providerId, String username,
                                     boolean approved, String action) {
        AppUser manager = requireCenterManager(username, institutionId);
        Provider provider = requireInstitutionProvider(institutionId, providerId);
        applyDecision(provider, approved, manager.getInstitution(), manager, action);
    }

    private void applyDecision(Provider provider, boolean approved, Institution institution,
                               AppUser actor, String action) {
        boolean reverification = provider.isPendingReverification();
        if (approved) {
            applyPendingValues(provider);
            provider.setVerificationStatus(VerificationStatus.VERIFIED);
            provider.setPublished(true);
        } else {
            discardPendingValues(provider);
            if (!reverification) {
                provider.setVerificationStatus(VerificationStatus.REJECTED);
                provider.setPublished(false);
            }
        }
        providerRepository.save(provider);

        if (provider.getUser() != null && !reverification) {
            provider.getUser().setVerified(approved);
            userRepository.save(provider.getUser());
        }

        recordVerification(provider, institution, actor,
                approved ? VerificationDecision.APPROVED : VerificationDecision.REJECTED, null);
        auditService.record(actor, action, "PROVIDER", provider.getId());
    }

    private void applyPendingValues(Provider provider) {
        if (!provider.isPendingReverification()) {
            return;
        }
        if (provider.getPendingName() != null) {
            provider.setName(provider.getPendingName());
        }
        if (provider.getPendingSpecialisation() != null) {
            provider.setSpecialisation(provider.getPendingSpecialisation());
        }
        if (provider.getPendingIdentityDetails() != null) {
            provider.setIdentityDetails(provider.getPendingIdentityDetails());
        }
        if (provider.getPendingQualifications() != null) {
            provider.setQualifications(provider.getPendingQualifications());
        }
        if (provider.getPendingLocation() != null) {
            provider.setLocation(provider.getPendingLocation());
        }
        if (provider.getPendingServiceMode() != null) {
            provider.setServiceMode(provider.getPendingServiceMode());
        }
        if (provider.getPendingService() != null) {
            provider.setService(provider.getPendingService());
        }
        clearPendingValues(provider);
    }

    private void discardPendingValues(Provider provider) {
        clearPendingValues(provider);
    }

    private void clearPendingValues(Provider provider) {
        provider.setPendingName(null);
        provider.setPendingSpecialisation(null);
        provider.setPendingIdentityDetails(null);
        provider.setPendingQualifications(null);
        provider.setPendingLocation(null);
        provider.setPendingServiceMode(null);
        provider.setPendingService(null);
        provider.setPendingReverification(false);
    }

    private void applyProfileUpdate(Provider provider, ProviderUpdateRequest request) {
        boolean verifiedFieldChanged = false;
        if (StringUtils.hasText(request.name())) {
            provider.setPendingName(request.name());
            verifiedFieldChanged = true;
        }
        if (StringUtils.hasText(request.specialisation())) {
            provider.setPendingSpecialisation(request.specialisation());
            verifiedFieldChanged = true;
        }
        if (StringUtils.hasText(request.identityDetails())) {
            provider.setPendingIdentityDetails(request.identityDetails());
            verifiedFieldChanged = true;
        }
        if (StringUtils.hasText(request.qualifications())) {
            provider.setPendingQualifications(request.qualifications());
            verifiedFieldChanged = true;
        }
        if (StringUtils.hasText(request.location())) {
            provider.setPendingLocation(request.location());
            verifiedFieldChanged = true;
        }
        if (request.serviceMode() != null) {
            provider.setPendingServiceMode(request.serviceMode());
            verifiedFieldChanged = true;
        }
        if (request.serviceId() != null) {
            provider.setPendingService(serviceRepository.findById(request.serviceId())
                    .orElseThrow(() -> new IllegalArgumentException("Service not found")));
            verifiedFieldChanged = true;
        }
        if (verifiedFieldChanged) {
            provider.setPendingReverification(true);
        }

        // Policy fields are captured per booking and do not affect the verified snapshot.
        if (request.responseDeadlineHours() != null) {
            provider.setResponseDeadlineHours(request.responseDeadlineHours());
        }
        if (request.cancellationRule() != null) {
            provider.setCancellationRule(request.cancellationRule());
        }
        if (request.minLeadTimeHours() != null) {
            provider.setMinLeadTimeHours(request.minLeadTimeHours());
        }
        if (request.cancellationCutoffHours() != null) {
            provider.setCancellationCutoffHours(request.cancellationCutoffHours());
        }
        if (StringUtils.hasText(request.noShowRule())) {
            provider.setNoShowRule(request.noShowRule());
        }
        if (request.noShowRestrictionHours() != null) {
            provider.setNoShowRestrictionHours(request.noShowRestrictionHours());
        }
        providerRepository.save(provider);
    }

    private Provider requireInstitutionProvider(Long institutionId, Long providerId) {
        Provider provider = providerRepository.findById(providerId)
                .orElseThrow(() -> new IllegalArgumentException("Provider not found"));
        if (provider.getInstitution() == null
                || !Objects.equals(provider.getInstitution().getId(), institutionId)) {
            throw new IllegalArgumentException("Provider does not belong to this institution");
        }
        return provider;
    }

    private ServiceEntity requireService(Long serviceId) {
        ServiceEntity service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new IllegalArgumentException("Service not found"));
        if (!service.isActive()) {
            throw new IllegalArgumentException("Service is not active");
        }
        return service;
    }

    private void requireIndependentService(ServiceEntity service) {
        if (service.getCategory() != ServiceCategory.COUNSELLING_COMFORTING
                && service.getCategory() != ServiceCategory.PASTORAL) {
            throw new IllegalArgumentException(
                    "Counsellors, comforters, mentors and pastors must be independent providers");
        }
    }

    private void requireInstitutionalService(ServiceEntity service) {
        if (service.getCategory() != ServiceCategory.INSTITUTIONAL
                && service.getCategory() != ServiceCategory.HEALTHCARE) {
            throw new IllegalArgumentException(
                    "Institutional and healthcare providers cannot be independent");
        }
    }

    private Provider buildProvider(AppUser user,
                                   Institution institution,
                                   ServiceEntity service,
                                   String name,
                                   String specialisation,
                                   ServiceMode serviceMode,
                                   Integer responseDeadlineHours,
                                   String cancellationRule) {
        Provider provider = new Provider();
        provider.setUser(user);
        provider.setInstitution(institution);
        provider.setService(service);
        provider.setName(name);
        provider.setSpecialisation(specialisation);
        provider.setServiceMode(serviceMode);
        provider.setVerificationStatus(VerificationStatus.PENDING);
        provider.setResponseDeadlineHours(responseDeadlineHours);
        provider.setCancellationRule(cancellationRule);
        provider.setPublished(false);
        provider.setPendingReverification(false);
        provider.setActive(true);
        return provider;
    }

    private void applyRegistrationDetails(Provider provider, String location, String identityDetails,
                                          String qualifications, Integer minLeadTimeHours,
                                          Integer cancellationCutoffHours, String noShowRule,
                                          Integer noShowRestrictionHours) {
        provider.setLocation(location);
        provider.setIdentityDetails(identityDetails);
        provider.setQualifications(qualifications);
        provider.setMinLeadTimeHours(minLeadTimeHours == null ? 0 : minLeadTimeHours);
        provider.setCancellationCutoffHours(cancellationCutoffHours == null ? 24 : cancellationCutoffHours);
        if (StringUtils.hasText(noShowRule)) {
            provider.setNoShowRule(noShowRule);
        }
        provider.setNoShowRestrictionHours(noShowRestrictionHours == null ? 24 : noShowRestrictionHours);
    }

    private Provider linkAndSave(AppUser user, Provider provider) {
        Provider saved = providerRepository.save(provider);
        user.setProvider(saved);
        userRepository.save(user);
        return saved;
    }

    private void recordVerification(Provider provider, Institution institution,
                                    AppUser decidedBy, VerificationDecision decision) {
        recordVerification(provider, institution, decidedBy, decision, null);
    }

    private void recordVerification(Provider provider, Institution institution,
                                    AppUser decidedBy, VerificationDecision decision,
                                    String documentReference) {
        VerificationRecord previous = provider.getId() == null ? null
                : verificationRecordRepository.findTopByProviderIdOrderByIdDesc(provider.getId())
                        .orElse(null);
        String reference = StringUtils.hasText(documentReference)
                ? documentReference
                : (previous == null ? null : previous.getDocumentReference());

        VerificationRecord record = new VerificationRecord();
        record.setProvider(provider);
        record.setInstitution(institution);
        record.setIdentityDetails(provider.getIdentityDetails());
        record.setQualifications(provider.getQualifications());
        record.setDocumentReference(reference);
        record.setCredentialReference(StringUtils.hasText(reference)
                ? reference
                : (provider.getUser() == null ? null : provider.getUser().getPhone()));
        record.setDecision(decision);
        record.setDecidedBy(decidedBy);
        if (decision != VerificationDecision.PENDING) {
            record.setDecidedAt(LocalDateTime.now());
        }
        verificationRecordRepository.save(record);
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

    private AppUser requireCenterManager(String username, Long institutionId) {
        if (username == null || username.isBlank()) {
            throw new ForbiddenException("Center manager access is required");
        }
        AppUser user = currentUserService.require(username);
        if (user.getAccountType() != AccountType.CENTER_MANAGER) {
            throw new ForbiddenException("Center manager access is required");
        }
        if (user.getInstitution() == null
                || !Objects.equals(user.getInstitution().getId(), institutionId)) {
            throw new ForbiddenException("Institution access is required");
        }
        if (user.getInstitution().getStatus() != InstitutionStatus.APPROVED) {
            throw new ForbiddenException("Institution is not approved");
        }
        return user;
    }

    private ManagedProviderView toView(Provider provider) {
        return new ManagedProviderView(
                provider.getId(),
                provider.getUser() == null ? null : provider.getUser().getId(),
                provider.getName(),
                provider.getUser() == null ? null : provider.getUser().getPhone(),
                provider.getService() == null ? null : provider.getService().getCategory(),
                provider.getService() == null ? null : provider.getService().getServiceType(),
                provider.getServiceMode(),
                provider.getVerificationStatus(),
                provider.getResponseDeadlineHours(),
                provider.getCancellationRule(),
                provider.getInstitution() == null ? null : provider.getInstitution().getName(),
                provider.isPublished(),
                provider.isPendingReverification(),
                provider.getLocation(),
                provider.getMinLeadTimeHours(),
                provider.getCancellationCutoffHours()
        );
    }
}
