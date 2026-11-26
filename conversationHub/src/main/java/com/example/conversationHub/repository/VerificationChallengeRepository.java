package com.example.conversationHub.repository;

import com.example.conversationHub.domain.VerificationChallenge;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VerificationChallengeRepository extends JpaRepository<VerificationChallenge, Long> {

    List<VerificationChallenge> findByUserIdOrderByCreatedAtDesc(Long userId);
}
