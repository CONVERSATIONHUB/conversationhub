package com.example.conversationHub.service;

import com.example.conversationHub.domain.AppUser;
import com.example.conversationHub.domain.enums.AccountType;
import com.example.conversationHub.domain.enums.VerificationChallengeType;
import com.example.conversationHub.domain.enums.VerificationChannel;
import com.example.conversationHub.repository.AppUserRepository;
import com.example.conversationHub.web.dto.AuthChallengeResponse;
import com.example.conversationHub.web.dto.AuthRegistrationResponse;
import com.example.conversationHub.web.dto.RegisterSeekerRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class SeekerRegistrationService {

    private final AccountService accountService;
    private final IdentityService identityService;
    private final AppUserRepository userRepository;

    public SeekerRegistrationService(AccountService accountService,
                                     IdentityService identityService,
                                     AppUserRepository userRepository) {
        this.accountService = accountService;
        this.identityService = identityService;
        this.userRepository = userRepository;
    }

    /** Backwards-compatible entry point returning only the new user id. */
    @Transactional
    public Long registerSeeker(RegisterSeekerRequest request) {
        return registerWithVerification(request).userId();
    }

    /**
     * Creates an unverified seeker and returns the registration challenge. Release 1 has no
     * SMS/email transport, so the generated code is included in the API response.
     */
    @Transactional
    public AuthRegistrationResponse registerWithVerification(RegisterSeekerRequest request) {
        AppUser user = accountService.createUser(
                request.name(),
                request.phone(),
                request.email(),
                request.password(),
                AccountType.SEEKER,
                "SEEKER",
                request.languageCode(),
                null,
                false
        );

        user.setConsentGiven(request.consentGiven());
        if (request.consentGiven()) {
            user.setConsentAt(LocalDateTime.now());
        }
        userRepository.save(user);

        AuthChallengeResponse challenge = identityService.createChallenge(
                user, VerificationChannel.PHONE, VerificationChallengeType.REGISTRATION, user.getPhone());
        return new AuthRegistrationResponse(user.getId(), challenge.challengeId(),
                challenge.code(), challenge.channel());
    }
}
