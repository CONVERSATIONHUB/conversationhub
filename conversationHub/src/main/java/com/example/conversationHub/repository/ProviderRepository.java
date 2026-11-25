package com.example.conversationHub.repository;

import com.example.conversationHub.domain.Provider;
import com.example.conversationHub.domain.enums.ServiceCategory;
import com.example.conversationHub.domain.enums.VerificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProviderRepository extends JpaRepository<Provider, Long> {
    Optional<Provider> findByUserId(Long userId);

    List<Provider> findByVerificationStatus(VerificationStatus status);

    List<Provider> findByInstitution_Id(Long institutionId);

    List<Provider> findByInstitution_IdAndVerificationStatus(
            Long institutionId, VerificationStatus status);

    List<Provider> findByInstitutionIsNullAndVerificationStatus(VerificationStatus status);

    List<Provider> findByService_Category(ServiceCategory category);
}
