package com.example.conversationHub.repository;

import com.example.conversationHub.domain.AppUser;
import com.example.conversationHub.domain.enums.AccountType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    @EntityGraph(attributePaths = "role")
    Optional<AppUser> findByPhone(String phone);

    @EntityGraph(attributePaths = "role")
    Optional<AppUser> findByEmail(String email);

    boolean existsByPhone(String phone);

    boolean existsByEmail(String email);

    List<AppUser> findByInstitution_Id(Long institutionId);

    List<AppUser> findByInstitution_IdAndAccountTypeAndVerifiedFalse(
            Long institutionId, AccountType accountType);

    List<AppUser> findByAccountType(AccountType accountType);
}
