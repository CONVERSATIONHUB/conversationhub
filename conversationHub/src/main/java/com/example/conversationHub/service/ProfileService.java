package com.example.conversationHub.service;

import com.example.conversationHub.domain.AppUser;
import com.example.conversationHub.domain.DeletionRequest;
import com.example.conversationHub.domain.LanguagePreference;
import com.example.conversationHub.domain.enums.DeletionStatus;
import com.example.conversationHub.domain.enums.LanguageCode;
import com.example.conversationHub.repository.DeletionRequestRepository;
import com.example.conversationHub.repository.LanguagePreferenceRepository;
import com.example.conversationHub.web.dto.AuthChallengeResponse;
import com.example.conversationHub.web.dto.ContactChangeRequest;
import com.example.conversationHub.web.dto.DeletionRequestView;
import com.example.conversationHub.web.dto.LanguagePreferenceRequest;
import com.example.conversationHub.web.dto.UserProfileView;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class ProfileService {

    private final CurrentUserService currentUserService;
    private final LanguagePreferenceRepository languagePreferenceRepository;
    private final IdentityService identityService;
    private final DeletionRequestRepository deletionRequestRepository;
    private final AuditService auditService;

    public ProfileService(CurrentUserService currentUserService,
                          LanguagePreferenceRepository languagePreferenceRepository,
                          IdentityService identityService,
                          DeletionRequestRepository deletionRequestRepository,
                          AuditService auditService) {
        this.currentUserService = currentUserService;
        this.languagePreferenceRepository = languagePreferenceRepository;
        this.identityService = identityService;
        this.deletionRequestRepository = deletionRequestRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public UserProfileView me(String username) {
        AppUser user = currentUserService.require(username);
        LanguageCode language = languagePreferenceRepository.findByUserId(user.getId())
                .map(LanguagePreference::getLanguageCode)
                .orElse(LanguageCode.EN);
        return new UserProfileView(
                user.getId(),
                user.getName(),
                user.getPhone(),
                user.getEmail(),
                user.getAccountType(),
                user.isVerified(),
                user.getInstitution() == null ? null : user.getInstitution().getId(),
                user.getProvider() == null ? null : user.getProvider().getId(),
                language
        );
    }

    public void updateLanguage(String username, LanguagePreferenceRequest request) {
        AppUser user = currentUserService.require(username);
        LanguagePreference preference = languagePreferenceRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    LanguagePreference created = new LanguagePreference();
                    created.setUser(user);
                    return created;
                });
        preference.setLanguageCode(request.languageCode());
        languagePreferenceRepository.save(preference);
    }

    public AuthChallengeResponse requestContactChange(String username, ContactChangeRequest request) {
        return identityService.requestContactChange(username, request);
    }

    public AppUser verifyContactChange(String username, Long challengeId, String code) {
        return identityService.verifyContactChange(username, challengeId, code);
    }

    public Long requestDeletion(String username, String reason) {
        AppUser user = currentUserService.require(username);
        deletionRequestRepository.findByUserIdAndStatus(user.getId(), DeletionStatus.PENDING)
                .ifPresent(existing -> {
                    throw new IllegalStateException("A deletion request is already pending");
                });
        DeletionRequest request = new DeletionRequest();
        request.setUser(user);
        request.setStatus(DeletionStatus.PENDING);
        request.setReason(reason);
        DeletionRequest saved = deletionRequestRepository.save(request);
        auditService.record(user, "DELETION_REQUESTED", "DELETION_REQUEST", saved.getId());
        return saved.getId();
    }

    @Transactional(readOnly = true)
    public DeletionRequestView latestDeletionRequest(String username) {
        AppUser user = currentUserService.require(username);
        return deletionRequestRepository.findByUserIdOrderByRequestedAtDesc(user.getId()).stream()
                .findFirst()
                .map(ProfileService::toView)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public List<DeletionRequestView> deletionRequests(String username) {
        AppUser user = currentUserService.require(username);
        return deletionRequestRepository.findByUserIdOrderByRequestedAtDesc(user.getId()).stream()
                .map(ProfileService::toView)
                .toList();
    }

    static DeletionRequestView toView(DeletionRequest request) {
        return new DeletionRequestView(
                request.getId(),
                request.getUser() == null ? null : request.getUser().getId(),
                request.getStatus() == null ? null : request.getStatus().name(),
                request.getReason(),
                request.getRequestedAt(),
                request.getResolvedAt()
        );
    }
}
