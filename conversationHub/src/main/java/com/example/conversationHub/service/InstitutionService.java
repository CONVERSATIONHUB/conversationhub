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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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

    public Long registerInstitution(RegisterInstitutionRequest request) {
        InstitutionRequest details = request.institution();

        Institution institution = new Institution();
        institution.setName(details.name());
        institution.setType(details.type());
        institution.setLocation(details.location());
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
        auditService.record(null, "INSTITUTION_REGISTERED", "INSTITUTION", saved.getId());
        return saved.getId();
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

    private AppUser requireAdmin(String username) {
        AppUser user = currentUserService.require(username);
        if (user.getAccountType() != AccountType.MAIN_ADMIN) {
            throw new ForbiddenException("Main administrator access is required");
        }
        return user;
    }

    private InstitutionView toView(Institution institution) {
        return new InstitutionView(
                institution.getId(),
                institution.getName(),
                institution.getType(),
                institution.getLocation(),
                institution.getStatus(),
                institution.getManager() == null ? null : institution.getManager().getName()
        );
    }
}
