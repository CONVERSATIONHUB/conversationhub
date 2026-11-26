package com.example.conversationHub.domain;

import com.example.conversationHub.domain.enums.ServiceMode;
import com.example.conversationHub.domain.enums.VerificationStatus;
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
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "provider")
@Getter
@Setter
@NoArgsConstructor
public class Provider {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true)
    private AppUser user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "institution_id")
    private Institution institution;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id", nullable = false)
    private ServiceEntity service;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 200)
    private String specialisation;

    @Enumerated(EnumType.STRING)
    @Column(name = "service_mode", nullable = false, length = 20,
            columnDefinition = "ENUM('IN_PERSON','REMOTE','BOTH')")
    private ServiceMode serviceMode = ServiceMode.IN_PERSON;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false, length = 20,
            columnDefinition = "ENUM('UNVERIFIED','PENDING','VERIFIED','REJECTED')")
    private VerificationStatus verificationStatus = VerificationStatus.UNVERIFIED;

    /**
     * Only published, verified and active providers of approved institutions are visible publicly.
     * A provider is published when its verification (or re-verification) succeeds.
     */
    @Column(nullable = false)
    private boolean published = false;

    @Column(length = 200)
    private String location;

    @Column(name = "identity_details", length = 2000)
    private String identityDetails;

    @Column(length = 2000)
    private String qualifications;

    @Column(name = "response_deadline_hours", nullable = false)
    private Integer responseDeadlineHours = 48;

    @Column(name = "cancellation_rule", nullable = false, length = 500)
    private String cancellationRule = "Cancellation allowed before the appointment.";

    @Column(name = "min_lead_time_hours", nullable = false)
    private Integer minLeadTimeHours = 0;

    @Column(name = "cancellation_cutoff_hours", nullable = false)
    private Integer cancellationCutoffHours = 24;

    @Column(name = "no_show_rule", nullable = false, length = 500)
    private String noShowRule = "A no-show may lead to a temporary booking restriction.";

    @Column(name = "no_show_restriction_hours", nullable = false)
    private Integer noShowRestrictionHours = 24;

    @Column(nullable = false)
    private boolean active = true;

    /*
     * FR-4.7: changes to verified fields are held here until re-verification succeeds.
     * The live fields above continue to represent the published snapshot in the meantime.
     */
    @Column(name = "pending_reverification", nullable = false)
    private boolean pendingReverification = false;

    @Column(name = "pending_name", length = 150)
    private String pendingName;

    @Column(name = "pending_specialisation", length = 200)
    private String pendingSpecialisation;

    @Column(name = "pending_identity_details", length = 2000)
    private String pendingIdentityDetails;

    @Column(name = "pending_qualifications", length = 2000)
    private String pendingQualifications;

    @Column(name = "pending_location", length = 200)
    private String pendingLocation;

    @Enumerated(EnumType.STRING)
    @Column(name = "pending_service_mode", length = 20)
    private ServiceMode pendingServiceMode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pending_service_id")
    private ServiceEntity pendingService;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
