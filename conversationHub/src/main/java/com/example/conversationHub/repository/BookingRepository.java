package com.example.conversationHub.repository;

import com.example.conversationHub.domain.Booking;
import com.example.conversationHub.domain.enums.BookingStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from Booking b where b.id = :id")
    Optional<Booking> findByIdForUpdate(@Param("id") Long id);

    List<Booking> findBySeekerId(Long seekerId);

    List<Booking> findByProviderId(Long providerId);

    List<Booking> findByProvider_Institution_Id(Long institutionId);

    List<Booking> findByStatus(BookingStatus status);

    List<Booking> findByExpiresAtBeforeAndStatus(LocalDateTime time, BookingStatus status);

    /**
     * Expiry scan loads and locks all due pending rows in one query so the status
     * can be re-checked while holding the lock (NFR-3.3).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from Booking b where b.status = :status and b.expiresAt < :now")
    List<Booking> findExpiringForUpdate(@Param("now") LocalDateTime now,
                                        @Param("status") BookingStatus status);
}
