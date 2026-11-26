package com.example.conversationHub.repository;

import com.example.conversationHub.domain.VerificationRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VerificationRecordRepository extends JpaRepository<VerificationRecord, Long> {
    List<VerificationRecord> findByProviderId(Long providerId);

    List<VerificationRecord> findByInstitutionId(Long institutionId);

    Optional<VerificationRecord> findTopByProviderIdOrderByIdDesc(Long providerId);
}
