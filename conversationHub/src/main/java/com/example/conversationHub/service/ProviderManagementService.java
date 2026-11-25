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
import com.example.conversationHub.web.dto.RegisterIndependentProviderRequest;
import com.example.conversationHub.web.exception.ForbiddenException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        Provider saved = linkAndSave(user, provider);
        recordVerification(saved, null, null, VerificationDecision.PENDING);
        auditService.record(null, "INDEPENDENT_PROVIDER_REGISTERED", "PROVIDER", saved.getId());
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
        Provider saved = linkAndSave(user, provider);
        recordVerification(saved, manager.getInstitution(), null, VerificationDecision.PENDING);
        auditService.record(manager, "INSTITUTIONAL_PROVIDER_CREATED", "PROVIDER", saved.getId());
        return saved.getId();
    }

    @Transactional(readOnly = true)
    public List<ManagedProviderView> listPendingIndependentProviders(String username) {
        requireAdmin(username);
        return providerRepository
                .findByInstitutionIsNullAndVerificationStatus(VerificationStatus.PENDING).stream()
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
        Provider provider = providerRepository.findById(providerId)
                .orElseThrow(() -> new IllegalArgumentException("Provider not found"));
        if (provider.getInstitution() == null
                || !Objects.equals(provider.getInstitution().getId(), institutionId)) {
            throw new IllegalArgumentException("Provider does not belong to this institution");
        }
        applyDecision(provider, approved, manager.getInstitution(), manager, action);
    }

    private void applyDecision(Provider provider, boolean approved, Institution institution,
                               AppUser actor, String action) {
        provider.setVerificationStatus(approved
                ? VerificationStatus.VERIFIED : VerificationStatus.REJECTED);
        providerRepository.save(provider);

        if (provider.getUser() != null) {
            provider.getUser().setVerified(approved);
            userRepository.save(provider.getUser());
        }

        recordVerification(provider, institution, actor,
                approved ? VerificationDecision.APPROVED : VerificationDecision.REJECTED);
        auditService.record(actor, action, "PROVIDER", provider.getId());
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
        provider.setActive(true);
        return provider;
    }

    private Provider linkAndSave(AppUser user, Provider provider) {
        Provider saved = providerRepository.save(provider);
        user.setProvider(saved);
        userRepository.save(user);
        return saved;
    }

    private void recordVerification(Provider provider, Institution institution,
                                    AppUser decidedBy, VerificationDecision decision) {
        VerificationRecord record = new VerificationRecord();
        record.setProvider(provider);
        record.setInstitution(institution);
        record.setCredentialReference(provider.getUser() == null
                ? null : provider.getUser().getPhone());
        record.setDecision(decision);
        record.setDecidedBy(decidedBy);
        if (decision != VerificationDecision.PENDING) {
            record.setDecidedAt(LocalDateTime.now());
        }
        verificationRecordRepository.save(record);
    }

    private AppUser requireAdmin(String username) {
        AppUser user = currentUserService.require(username);
        if (user.getAccountType() != AccountType.MAIN_ADMIN) {
            throw new ForbiddenException("Main administrator access is required");
        }
        return user;
    }

    private AppUser requireCenterManager(String username, Long institutionId) {
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
                provider.getInstitution() == null ? null : provider.getInstitution().getName()
        );
    }
}
