package com.example.conversationHub.repository;

import com.example.conversationHub.domain.Institution;
import com.example.conversationHub.domain.enums.InstitutionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InstitutionRepository extends JpaRepository<Institution, Long> {
    List<Institution> findByStatus(InstitutionStatus status);
}
