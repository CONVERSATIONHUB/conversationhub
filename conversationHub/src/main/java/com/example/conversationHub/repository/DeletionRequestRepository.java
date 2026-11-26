package com.example.conversationHub.repository;

import com.example.conversationHub.domain.DeletionRequest;
import com.example.conversationHub.domain.enums.DeletionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DeletionRequestRepository extends JpaRepository<DeletionRequest, Long> {

    List<DeletionRequest> findByUserIdOrderByRequestedAtDesc(Long userId);

    List<DeletionRequest> findByStatusOrderByRequestedAtAsc(DeletionStatus status);

    Optional<DeletionRequest> findByUserIdAndStatus(Long userId, DeletionStatus status);
}
