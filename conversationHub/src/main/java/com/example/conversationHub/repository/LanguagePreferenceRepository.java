package com.example.conversationHub.repository;

import com.example.conversationHub.domain.LanguagePreference;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LanguagePreferenceRepository extends JpaRepository<LanguagePreference, Long> {
    Optional<LanguagePreference> findByUserId(Long userId);
}
