package com.example.conversationHub.repository;

import com.example.conversationHub.domain.RescheduleRequest;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RescheduleRequestRepository extends JpaRepository<RescheduleRequest, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from RescheduleRequest r where r.id = :id")
    Optional<RescheduleRequest> findByIdForUpdate(@Param("id") Long id);

    List<RescheduleRequest> findByBookingIdOrderByRequestedAtDesc(Long bookingId);

    List<RescheduleRequest> findByBooking_Seeker_IdOrderByRequestedAtDesc(Long seekerId);

    boolean existsByBookingIdAndStatus(Long bookingId, RescheduleRequest.RescheduleStatus status);
}
