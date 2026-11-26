package com.example.conversationHub.service;

import com.example.conversationHub.domain.AppUser;
import com.example.conversationHub.domain.Institution;
import com.example.conversationHub.domain.enums.AccountType;
import com.example.conversationHub.domain.enums.InstitutionStatus;
import com.example.conversationHub.repository.InstitutionRepository;
import com.example.conversationHub.web.dto.InstitutionRequest;
import com.example.conversationHub.web.dto.InstitutionView;
import com.example.conversationHub.web.dto.RegisterInstitutionRequest;
import com.example.conversationHub.web.exception.ForbiddenException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@Transactional
public class InstitutionService {

    private final InstitutionRepository institutionRepository;
    private final AccountService accountService;
    private final AuditService auditService;
    private final CurrentUserService currentUserService;

    public InstitutionService(InstitutionRepository institutionRepository,
                              AccountService accountService,
                              AuditService auditService,
                              CurrentUserService currentUserService) {
        this.institutionRepository = institutionRepository;
        this.accountService = accountService;
        this.auditService = auditService;
        this.currentUserService = currentUserService;
    }

    /**
     * Kept for the existing /api/auth/register/institution route. The route is permitted to
     * guests by the security configuration, so authorization is enforced here (FR-3.1).
     */
    public Long registerInstitution(RegisterInstitutionRequest request) {
        return registerInstitution(request, authenticatedUsername());
    }

    public Long registerInstitution(RegisterInstitutionRequest request, String username) {
        AppUser admin = requireAdmin(username);
        InstitutionRequest details = request.institution();

        Institution institution = new Institution();
        institution.setName(details.name());
        institution.setType(details.type());
        institution.setLocation(details.location());
        institution.setContactPhone(details.contactPhone());
        institution.setContactEmail(details.contactEmail());
        institution.setContactAddress(details.contactAddress());
        institution.setStatus(InstitutionStatus.PENDING);
        Institution saved = institutionRepository.save(institution);

        AppUser manager = accountService.createUser(
                request.managerName(),
                request.managerPhone(),
                request.managerEmail(),
                request.managerPassword(),
                AccountType.CENTER_MANAGER,
                "CENTER_MANAGER",
                request.languageCode(),
                saved,
                false
        );

        saved.setManager(manager);
        institutionRepository.save(saved);
        auditService.record(admin, "INSTITUTION_REGISTERED", "INSTITUTION", saved.getId());
        return saved.getId();
    }

    /** FR-2.5: manager-scoped update of the institution account details. */
    public void updateInstitution(Long institutionId, String username, InstitutionRequest request) {
        AppUser actor = currentUserService.require(username);
        Institution institution = institutionRepository.findById(institutionId)
                .orElseThrow(() -> new IllegalArgumentException("Institution not found"));
        requireManagerOrAdmin(actor, institution);

        if (request.name() != null) {
            institution.setName(request.name());
        }
        if (request.type() != null) {
            institution.setType(request.type());
        }
        if (request.location() != null) {
            institution.setLocation(request.location());
        }
        if (request.contactPhone() != null) {
            institution.setContactPhone(request.contactPhone());
        }
        if (request.contactEmail() != null) {
            institution.setContactEmail(request.contactEmail());
        }
        if (request.contactAddress() != null) {
            institution.setContactAddress(request.contactAddress());
        }
        institutionRepository.save(institution);
        auditService.record(actor, "INSTITUTION_UPDATED", "INSTITUTION", institution.getId());
    }

    @Transactional(readOnly = true)
    public List<InstitutionView> listPending() {
        return listByStatus(InstitutionStatus.PENDING);
    }

    @Transactional(readOnly = true)
    public List<InstitutionView> listByStatus(InstitutionStatus status) {
        return institutionRepository.findByStatus(status).stream()
                .map(this::toView)
                .toList();
    }

    public void approve(Long institutionId, String username) {
        changeStatus(institutionId, InstitutionStatus.APPROVED, username, "INSTITUTION_APPROVED");
    }

    public void reject(Long institutionId, String username) {
        changeStatus(institutionId, InstitutionStatus.REJECTED, username, "INSTITUTION_REJECTED");
    }

    public void changeStatus(Long institutionId, InstitutionStatus status, String username) {
        String action = status == InstitutionStatus.APPROVED ? "INSTITUTION_APPROVED"
                : status == InstitutionStatus.REJECTED ? "INSTITUTION_REJECTED"
                : "INSTITUTION_STATUS_CHANGED";
        changeStatus(institutionId, status, username, action);
    }

    private void changeStatus(Long institutionId, InstitutionStatus status,
                              String username, String action) {
        AppUser admin = requireAdmin(username);
        Institution institution = institutionRepository.findById(institutionId)
                .orElseThrow(() -> new IllegalArgumentException("Institution not found"));

        institution.setStatus(status);
        if (institution.getManager() != null) {
            institution.getManager().setVerified(status == InstitutionStatus.APPROVED);
        }
        institutionRepository.save(institution);
        auditService.record(admin, action, "INSTITUTION", institution.getId());
    }

    private String authenticatedUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getName())) {
            throw new ForbiddenException("Main administrator access is required");
        }
        return authentication.getName();
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

    private void requireManagerOrAdmin(AppUser actor, Institution institution) {
        if (actor.getAccountType() == AccountType.MAIN_ADMIN) {
            return;
        }
        if (actor.getAccountType() != AccountType.CENTER_MANAGER
                || institution.getManager() == null
                || !Objects.equals(institution.getManager().getId(), actor.getId())) {
            throw new ForbiddenException("Institution manager access is required");
        }
    }

    private InstitutionView toView(Institution institution) {
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
}
