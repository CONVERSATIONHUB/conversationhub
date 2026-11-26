package com.example.conversationHub.repository;

import com.example.conversationHub.domain.SlotRecurrence;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SlotRecurrenceRepository extends JpaRepository<SlotRecurrence, Long> {

    List<SlotRecurrence> findByProviderIdOrderByDayOfWeekAscStartTimeAsc(Long providerId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from SlotRecurrence r where r.provider.id = :providerId")
    List<SlotRecurrence> findByProviderIdForUpdate(@Param("providerId") Long providerId);
}
