package com.example.conversationHub.repository;

import com.example.conversationHub.domain.AvailabilitySlot;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AvailabilitySlotRepository extends JpaRepository<AvailabilitySlot, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from AvailabilitySlot s where s.id = :id")
    Optional<AvailabilitySlot> findByIdForUpdate(@Param("id") Long id);

    List<AvailabilitySlot> findByProviderIdAndStartTimeBetween(
            Long providerId, LocalDateTime from, LocalDateTime to);

    List<AvailabilitySlot> findByProviderIdAndStartTimeGreaterThanEqualAndEndTimeLessThanEqual(
            Long providerId, LocalDateTime from, LocalDateTime to);
}
