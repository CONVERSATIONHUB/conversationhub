package com.example.conversationHub.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * A seeker's request to move an existing booking to a different slot.
 * The same decider rules as a new booking request apply to the decision.
 */
@Entity
@Table(name = "reschedule_request")
@Getter
@Setter
@NoArgsConstructor
public class RescheduleRequest {

    public enum RescheduleStatus {
        PENDING,
        APPROVED,
        REJECTED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "new_slot_id", nullable = false)
    private AvailabilitySlot newSlot;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20,
            columnDefinition = "ENUM('PENDING','APPROVED','REJECTED')")
    private RescheduleStatus status = RescheduleStatus.PENDING;

    @Column(name = "capacity_held", nullable = false)
    private boolean capacityHeld = true;

    @Column(length = 500)
    private String reason;

    @Column(name = "decision_reason", length = 500)
    private String decisionReason;

    @Column(name = "requested_at", nullable = false, updatable = false)
    private LocalDateTime requestedAt;

    @Column(name = "decided_at")
    private LocalDateTime decidedAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (requestedAt == null) {
            requestedAt = now;
        }
        if (status == null) {
            status = RescheduleStatus.PENDING;
        }
    }
}
