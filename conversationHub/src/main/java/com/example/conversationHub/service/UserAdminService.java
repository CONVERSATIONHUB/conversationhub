package com.example.conversationHub.service;

import com.example.conversationHub.domain.AppRole;
import com.example.conversationHub.domain.AppUser;
import com.example.conversationHub.domain.DeletionRequest;
import com.example.conversationHub.domain.enums.AccountType;
import com.example.conversationHub.domain.enums.DeletionStatus;
import com.example.conversationHub.repository.AppRoleRepository;
import com.example.conversationHub.repository.AppUserRepository;
import com.example.conversationHub.repository.DeletionRequestRepository;
import com.example.conversationHub.web.dto.DeletionRequestView;
import com.example.conversationHub.web.dto.UserAdminView;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** Main-administrator user management and deletion review (FR-2.8, FR-2.11, NFR-5.5). */
@Service
@Transactional
public class UserAdminService {

    private final AppUserRepository userRepository;
    private final AppRoleRepository roleRepository;
    private final DeletionRequestRepository deletionRequestRepository;
    private final CurrentUserService currentUserService;
    private final AuditService auditService;
    private final PasswordEncoder passwordEncoder;

    public UserAdminService(AppUserRepository userRepository,
                            AppRoleRepository roleRepository,
                            DeletionRequestRepository deletionRequestRepository,
                            CurrentUserService currentUserService,
                            AuditService auditService,
                            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.deletionRequestRepository = deletionRequestRepository;
        this.currentUserService = currentUserService;
        this.auditService = auditService;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<UserAdminView> listUsers() {
        return userRepository.findAll().stream().map(UserAdminService::toView).toList();
    }

    public void assignRole(Long userId, AccountType accountType, String roleName, String actorUsername) {
        AppUser actor = currentUserService.require(actorUsername);
        AppUser user = requireUser(userId);
        String resolvedRoleName = roleName == null || roleName.isBlank()
                ? accountType.name() : roleName.trim();
        AppRole role = roleRepository.findByName(resolvedRoleName)
                .orElseThrow(() -> new IllegalStateException("Role is missing from the database: " + resolvedRoleName));
        user.setAccountType(accountType);
        user.setRole(role);
        userRepository.save(user);
        auditService.record(actor, "ROLE_ASSIGNED", "APP_USER", user.getId());
    }

    public void setActive(Long userId, boolean active, String actorUsername) {
        AppUser actor = currentUserService.require(actorUsername);
        AppUser user = requireUser(userId);
        user.setActive(active);
        userRepository.save(user);
        auditService.record(actor, active ? "ACCOUNT_ACTIVATED" : "ACCOUNT_DEACTIVATED",
                "APP_USER", user.getId());
    }

    @Transactional(readOnly = true)
    public List<DeletionRequestView> listDeletionRequests() {
        return deletionRequestRepository.findAll().stream()
                .map(ProfileService::toView)
                .toList();
    }

    public void rejectDeletionRequest(Long requestId, String actorUsername) {
        AppUser actor = currentUserService.require(actorUsername);
        DeletionRequest request = requireRequest(requestId);
        if (request.getStatus() != DeletionStatus.PENDING) {
            throw new IllegalStateException("Deletion request has already been resolved");
        }
        request.setStatus(DeletionStatus.REJECTED);
        request.setResolvedAt(LocalDateTime.now());
        request.setResolvedBy(actor);
        deletionRequestRepository.save(request);
        auditService.record(actor, "DELETION_REJECTED", "DELETION_REQUEST", request.getId());
    }

    /** Approves a request, anonymising the account while keeping its booking history. */
    public void approveDeletionRequest(Long requestId, String actorUsername) {
        AppUser actor = currentUserService.require(actorUsername);
        DeletionRequest request = requireRequest(requestId);
        if (request.getStatus() != DeletionStatus.PENDING) {
            throw new IllegalStateException("Deletion request has already been resolved");
        }
        AppUser user = request.getUser();
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        user.setName("Deleted User");
        user.setPhone("del-" + user.getId() + "-" + suffix);
        user.setEmail("deleted-" + user.getId() + "-" + suffix + "@invalid.local");
        user.setPasswordHash(passwordEncoder.encode(UUID.randomUUID().toString()));
        user.setVerified(false);
        user.setActive(false);
        user.setConsentGiven(false);
        user.setConsentAt(null);
        userRepository.save(user);

        request.setStatus(DeletionStatus.APPROVED);
        request.setResolvedAt(LocalDateTime.now());
        request.setResolvedBy(actor);
        deletionRequestRepository.save(request);
        auditService.record(actor, "ACCOUNT_ANONYMISED", "APP_USER", user.getId());
        auditService.record(actor, "DELETION_APPROVED", "DELETION_REQUEST", request.getId());
    }

    private AppUser requireUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
    }

    private DeletionRequest requireRequest(Long requestId) {
        return deletionRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Deletion request not found: " + requestId));
    }

    static UserAdminView toView(AppUser user) {
        return new UserAdminView(
                user.getId(),
                user.getName(),
                user.getPhone(),
                user.getEmail(),
                user.getAccountType(),
                user.getRole() == null ? null : user.getRole().getName(),
                user.isVerified(),
                user.isActive(),
                user.getInstitution() == null ? null : user.getInstitution().getId(),
                user.getProvider() == null ? null : user.getProvider().getId()
        );
    }
}
