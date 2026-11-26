package com.example.conversationHub.service;

import com.example.conversationHub.domain.AppUser;
import com.example.conversationHub.domain.enums.AccountType;
import com.example.conversationHub.web.dto.RegisterSeekerRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SeekerRegistrationService {

    private final AccountService accountService;

    public SeekerRegistrationService(AccountService accountService) {
        this.accountService = accountService;
    }

    @Transactional
    public Long registerSeeker(RegisterSeekerRequest request) {
        // The first release has no SMS/email channel, so format and uniqueness
        // checks are the verification applied at registration.
        AppUser user = accountService.createUser(
                request.name(),
                request.phone(),
                request.email(),
                request.password(),
                AccountType.SEEKER,
                "SEEKER",
                request.languageCode(),
                null,
                true
        );
        return user.getId();
    }
}
