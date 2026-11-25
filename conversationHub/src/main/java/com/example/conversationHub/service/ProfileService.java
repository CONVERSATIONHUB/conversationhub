package com.example.conversationHub.service;

import com.example.conversationHub.domain.AppUser;
import com.example.conversationHub.domain.LanguagePreference;
import com.example.conversationHub.domain.enums.LanguageCode;
import com.example.conversationHub.repository.LanguagePreferenceRepository;
import com.example.conversationHub.web.dto.LanguagePreferenceRequest;
import com.example.conversationHub.web.dto.UserProfileView;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ProfileService {

    private final CurrentUserService currentUserService;
    private final LanguagePreferenceRepository languagePreferenceRepository;

    public ProfileService(CurrentUserService currentUserService,
                          LanguagePreferenceRepository languagePreferenceRepository) {
        this.currentUserService = currentUserService;
        this.languagePreferenceRepository = languagePreferenceRepository;
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
}
