package com.example.conversationHub.service;

import com.example.conversationHub.domain.AppSetting;
import com.example.conversationHub.domain.AppUser;
import com.example.conversationHub.domain.enums.LanguageCode;
import com.example.conversationHub.repository.AppSettingRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** System-wide defaults (FR-11.5). Deliberately does not depend on AccountService. */
@Service
@Transactional
public class SystemSettingsService {

    public static final String DEFAULT_LANGUAGE_KEY = "default.language";

    private final AppSettingRepository settingRepository;
    private final AuditService auditService;
    private final CurrentUserService currentUserService;
    private final LanguageCode fallbackLanguage;

    public SystemSettingsService(AppSettingRepository settingRepository,
                                 AuditService auditService,
                                 CurrentUserService currentUserService,
                                 @Value("${app.default-language:EN}") String fallbackLanguage) {
        this.settingRepository = settingRepository;
        this.auditService = auditService;
        this.currentUserService = currentUserService;
        this.fallbackLanguage = parseLanguage(fallbackLanguage);
    }

    @Transactional(readOnly = true)
    public LanguageCode getDefaultLanguage() {
        return settingRepository.findBySettingKey(DEFAULT_LANGUAGE_KEY)
                .map(AppSetting::getSettingValue)
                .map(SystemSettingsService::parseLanguage)
                .orElse(fallbackLanguage);
    }

    public void setDefaultLanguage(LanguageCode languageCode, AppUser actor) {
        if (languageCode == null) {
            throw new IllegalArgumentException("Language is required");
        }
        AppSetting setting = settingRepository.findBySettingKey(DEFAULT_LANGUAGE_KEY)
                .orElseGet(() -> {
                    AppSetting created = new AppSetting();
                    created.setSettingKey(DEFAULT_LANGUAGE_KEY);
                    return created;
                });
        setting.setSettingValue(languageCode.name());
        settingRepository.save(setting);
        auditService.record(actor, "DEFAULT_LANGUAGE_UPDATED", "APP_SETTING", setting.getId());
    }

    public void setDefaultLanguage(LanguageCode languageCode, String actorUsername) {
        setDefaultLanguage(languageCode, currentUserService.require(actorUsername));
    }

    private static LanguageCode parseLanguage(String value) {
        if (value == null || value.isBlank()) {
            return LanguageCode.EN;
        }
        try {
            return LanguageCode.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return LanguageCode.EN;
        }
    }
}
