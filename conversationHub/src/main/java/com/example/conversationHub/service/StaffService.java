package com.example.conversationHub.service;

import com.example.conversationHub.domain.AppUser;
import com.example.conversationHub.domain.Institution;
import com.example.conversationHub.domain.VerificationRecord;
import com.example.conversationHub.domain.enums.AccountType;
import com.example.conversationHub.domain.enums.InstitutionStatus;
import com.example.conversationHub.domain.enums.VerificationDecision;
import com.example.conversationHub.repository.AppUserRepository;
import com.example.conversationHub.repository.VerificationRecordRepository;
import com.example.conversationHub.web.dto.CreateStaffRequest;
import com.example.conversationHub.web.dto.StaffView;
import com.example.conversationHub.web.exception.ForbiddenException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
@Transactional
public class StaffService {

    private final AppUserRepository userRepository;
    private final VerificationRecordRepository verificationRecordRepository;
    private final AccountService accountService;
    private final AuditService auditService;
    private final CurrentUserService currentUserService;

    public StaffService(AppUserRepository userRepository,
                        VerificationRecordRepository verificationRecordRepository,
                        AccountService accountService,
                        AuditService auditService,
                        CurrentUserService currentUserService) {
        this.userRepository = userRepository;
        this.verificationRecordRepository = verificationRecordRepository;
        this.accountService = accountService;
        this.auditService = auditService;
        this.currentUserService = currentUserService;
    }

    public Long createStaff(Long institutionId, String username, CreateStaffRequest request) {
        AppUser manager = requireCenterManager(username, institutionId);
        if (request.accountType() != AccountType.APPOINTMENT_OFFICER
                && request.accountType() != AccountType.CENTER_MANAGER) {
            throw new IllegalArgumentException("Only institution staff accounts can be created here");
        }

        AppUser staff = accountService.createUser(
                request.name(),
                request.phone(),
                request.email(),
                request.password(),
                request.accountType(),
                request.accountType().name(),
                request.languageCode(),
                manager.getInstitution(),
                false
        );

        VerificationRecord record = new VerificationRecord();
        record.setInstitution(manager.getInstitution());
        record.setCredentialReference(staff.getPhone());
        record.setDecision(VerificationDecision.PENDING);
        verificationRecordRepository.save(record);

        auditService.record(manager, "STAFF_CREATED", "APP_USER", staff.getId());
        return staff.getId();
    }

    @Transactional(readOnly = true)
    public List<StaffView> listStaff(Long institutionId, String username) {
        requireCenterManager(username, institutionId);
        return userRepository.findByInstitution_Id(institutionId).stream()
                .filter(this::isStaff)
                .map(this::toView)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<StaffView> listPendingStaff(Long institutionId, String username) {
        requireCenterManager(username, institutionId);
        return userRepository.findByInstitution_Id(institutionId).stream()
                .filter(this::isStaff)
                .filter(staff -> !staff.isVerified())
                .map(this::toView)
                .toList();
    }

    public void verify(Long institutionId, Long staffUserId, String username) {
        decide(institutionId, staffUserId, username, true, "STAFF_VERIFIED");
    }

    public void reject(Long institutionId, Long staffUserId, String username) {
        decide(institutionId, staffUserId, username, false, "STAFF_VERIFICATION_REJECTED");
    }

    private void decide(Long institutionId, Long staffUserId, String username,
                        boolean approved, String action) {
        AppUser manager = requireCenterManager(username, institutionId);
        AppUser staff = userRepository.findById(staffUserId)
                .orElseThrow(() -> new IllegalArgumentException("Staff account not found"));
        if (!isStaff(staff) || staff.getInstitution() == null
                || !Objects.equals(staff.getInstitution().getId(), institutionId)) {
            throw new IllegalArgumentException("Staff account does not belong to this institution");
        }
        if (Objects.equals(staff.getId(), manager.getId())) {
            throw new IllegalArgumentException("A manager cannot verify their own account");
        }

        staff.setVerified(approved);
        userRepository.save(staff);

        VerificationRecord record = new VerificationRecord();
        record.setInstitution(manager.getInstitution());
        record.setCredentialReference(staff.getPhone());
        record.setDecision(approved ? VerificationDecision.APPROVED : VerificationDecision.REJECTED);
        record.setDecidedBy(manager);
        record.setDecidedAt(LocalDateTime.now());
        verificationRecordRepository.save(record);

        auditService.record(manager, action, "APP_USER", staff.getId());
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

    private boolean isStaff(AppUser user) {
        return user.getAccountType() == AccountType.CENTER_MANAGER
                || user.getAccountType() == AccountType.APPOINTMENT_OFFICER;
    }

    private StaffView toView(AppUser user) {
        return new StaffView(
                user.getId(),
                user.getName(),
                user.getPhone(),
                user.getEmail(),
                user.getAccountType(),
                user.isVerified()
        );
    }
}
