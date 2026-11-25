package com.example.conversationHub.service;

import com.example.conversationHub.domain.AppRole;
import com.example.conversationHub.domain.AppUser;
import com.example.conversationHub.domain.Institution;
import com.example.conversationHub.domain.LanguagePreference;
import com.example.conversationHub.domain.enums.AccountType;
import com.example.conversationHub.domain.enums.LanguageCode;
import com.example.conversationHub.repository.AppRoleRepository;
import com.example.conversationHub.repository.AppUserRepository;
import com.example.conversationHub.repository.LanguagePreferenceRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountService {

    private final AppUserRepository userRepository;
    private final AppRoleRepository roleRepository;
    private final LanguagePreferenceRepository languagePreferenceRepository;
    private final PasswordEncoder passwordEncoder;

    public AccountService(AppUserRepository userRepository,
                          AppRoleRepository roleRepository,
                          LanguagePreferenceRepository languagePreferenceRepository,
                          PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.languagePreferenceRepository = languagePreferenceRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public AppUser createUser(String name,
                              String phone,
                              String email,
                              String password,
                              AccountType accountType,
                              String roleName,
                              LanguageCode languageCode,
                              Institution institution,
                              boolean verified) {
        if (userRepository.existsByPhone(phone)) {
            throw new IllegalArgumentException("Phone number is already registered");
        }
        if (email != null && !email.isBlank() && userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email address is already registered");
        }

        AppRole role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new IllegalStateException("Role is missing from the database: " + roleName));

        AppUser user = new AppUser();
        user.setName(name);
        user.setPhone(phone);
        user.setEmail(email == null || email.isBlank() ? null : email);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setVerified(verified);
        user.setAccountType(accountType);
        user.setRole(role);
        user.setInstitution(institution);

        AppUser saved = userRepository.save(user);

        LanguagePreference preference = new LanguagePreference();
        preference.setUser(saved);
        preference.setLanguageCode(languageCode == null ? LanguageCode.EN : languageCode);
        languagePreferenceRepository.save(preference);

        return saved;
    }
}
