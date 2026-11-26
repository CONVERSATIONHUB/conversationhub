package com.example.conversationHub;

import com.example.conversationHub.domain.AppUser;
import com.example.conversationHub.domain.AvailabilitySlot;
import com.example.conversationHub.domain.Booking;
import com.example.conversationHub.domain.LanguagePreference;
import com.example.conversationHub.domain.Notification;
import com.example.conversationHub.domain.Provider;
import com.example.conversationHub.domain.ServiceEntity;
import com.example.conversationHub.domain.enums.AccountType;
import com.example.conversationHub.domain.enums.BookingStatus;
import com.example.conversationHub.domain.enums.InstitutionType;
import com.example.conversationHub.domain.enums.LanguageCode;
import com.example.conversationHub.domain.enums.ServiceCategory;
import com.example.conversationHub.domain.enums.ServiceMode;
import com.example.conversationHub.repository.AppUserRepository;
import com.example.conversationHub.repository.AvailabilitySlotRepository;
import com.example.conversationHub.repository.BookingRepository;
import com.example.conversationHub.repository.LanguagePreferenceRepository;
import com.example.conversationHub.repository.NotificationRepository;
import com.example.conversationHub.repository.ProviderRepository;
import com.example.conversationHub.repository.ServiceRepository;
import com.example.conversationHub.security.UserDetailsServiceImpl;
import com.example.conversationHub.service.AvailabilityService;
import com.example.conversationHub.service.BookingService;
import com.example.conversationHub.service.IdentityService;
import com.example.conversationHub.service.InstitutionService;
import com.example.conversationHub.service.ProviderManagementService;
import com.example.conversationHub.service.ProviderProfileService;
import com.example.conversationHub.service.PublicCatalogService;
import com.example.conversationHub.service.ReportingService;
import com.example.conversationHub.service.SeekerRegistrationService;
import com.example.conversationHub.service.StaffService;
import com.example.conversationHub.service.SystemSettingsService;
import com.example.conversationHub.web.dto.AuthRegistrationResponse;
import com.example.conversationHub.web.dto.BookingView;
import com.example.conversationHub.web.dto.CreateBookingRequest;
import com.example.conversationHub.web.dto.CreateInstitutionalProviderRequest;
import com.example.conversationHub.web.dto.CreateStaffRequest;
import com.example.conversationHub.web.dto.DecisionRequest;
import com.example.conversationHub.web.dto.InstitutionRequest;
import com.example.conversationHub.web.dto.ProviderDetailView;
import com.example.conversationHub.web.dto.ProviderUpdateRequest;
import com.example.conversationHub.web.dto.RegisterIndependentProviderRequest;
import com.example.conversationHub.web.dto.RegisterInstitutionRequest;
import com.example.conversationHub.web.dto.RegisterSeekerRequest;
import com.example.conversationHub.web.dto.SlotRequest;
import com.example.conversationHub.web.exception.ForbiddenException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end backend coverage of the SRS workflows. Runs against the H2 test profile;
 * each test is rolled back so services and identifiers stay isolated.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class SrsBackendWorkflowTest {

    private static final String ADMIN = "+255700000000";
    private static final AtomicInteger SEQUENCE = new AtomicInteger();

    @Autowired private InstitutionService institutionService;
    @Autowired private StaffService staffService;
    @Autowired private ProviderManagementService providerManagementService;
    @Autowired private ProviderProfileService providerProfileService;
    @Autowired private AvailabilityService availabilityService;
    @Autowired private SeekerRegistrationService seekerRegistrationService;
    @Autowired private IdentityService identityService;
    @Autowired private BookingService bookingService;
    @Autowired private PublicCatalogService publicCatalogService;
    @Autowired private ReportingService reportingService;
    @Autowired private SystemSettingsService systemSettingsService;
    @Autowired private UserDetailsServiceImpl userDetailsService;
    @Autowired private AppUserRepository userRepository;
    @Autowired private ProviderRepository providerRepository;
    @Autowired private BookingRepository bookingRepository;
    @Autowired private AvailabilitySlotRepository slotRepository;
    @Autowired private NotificationRepository notificationRepository;
    @Autowired private LanguagePreferenceRepository languagePreferenceRepository;
    @Autowired private ServiceRepository serviceRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private MockMvc mockMvc;

    @Test
    void httpEndpointsEnforceGuestAndRoleBoundaries() throws Exception {
        String managerPhone = phone();
        String officerPhone = phone();
        Long institutionId = registerAndApproveInstitution(managerPhone);
        Long officerId = staffService.createStaff(institutionId, managerPhone,
                new CreateStaffRequest("Officer", officerPhone, null, "Pass12345!",
                        LanguageCode.EN, AccountType.APPOINTMENT_OFFICER));
        staffService.verify(institutionId, officerId, managerPhone);

        mockMvc.perform(get("/api/public/services")).andExpect(status().isOk());
        mockMvc.perform(get("/api/institutions/" + institutionId + "/reports"))
                .andExpect(status().isUnauthorized()); // API entry point challenges the guest
        mockMvc.perform(get("/api/institutions/" + institutionId + "/reports")
                        .header("Authorization", basicAuth(officerPhone, "Pass12345!")))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/admin/reports/platform")
                        .header("Authorization", basicAuth(officerPhone, "Pass12345!")))
                .andExpect(status().isForbidden());
    }

    @Test
    void seekerRegistrationRequiresVerificationConsentAndRecoveryWorks() {
        String phone = phone();
        RegisterSeekerRequest request = new RegisterSeekerRequest(
                "Seeker One", phone, email(), "Pass12345!", LanguageCode.EN, true);
        AuthRegistrationResponse registration = seekerRegistrationService.registerWithVerification(request);

        AppUser user = userRepository.findById(registration.userId()).orElseThrow();
        assertThat(user.getAccountType()).isEqualTo(AccountType.SEEKER);
        assertThat(user.isVerified()).isFalse();
        assertThat(user.isConsentGiven()).isTrue();
        assertThat(userDetailsService.loadUserByUsername(phone).isEnabled()).isFalse();

        String wrongCode = registration.code().equals("000000") ? "111111" : "000000";
        assertThatThrownBy(() -> identityService.verifyChallenge(registration.challengeId(), wrongCode))
                .isInstanceOf(IllegalArgumentException.class);
        identityService.verifyChallenge(registration.challengeId(), registration.code());
        assertThat(userRepository.findById(user.getId()).orElseThrow().isVerified()).isTrue();
        assertThatThrownBy(() -> identityService.verifyChallenge(registration.challengeId(), registration.code()))
                .isInstanceOf(IllegalArgumentException.class); // a challenge is single-use

        var reset = identityService.forgotPassword(phone);
        identityService.resetPassword(reset.token(), "NewPass123!");
        assertThat(passwordEncoder.matches("NewPass123!",
                userRepository.findById(user.getId()).orElseThrow().getPasswordHash())).isTrue();
        assertThatThrownBy(() -> identityService.resetPassword(reset.token(), "Again123!"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void contactChangeIsAppliedOnlyAfterVerificationAndDefaultLanguageIsConfigurable() {
        String phone = registerVerifiedSeeker();
        systemSettingsService.setDefaultLanguage(LanguageCode.SW, ADMIN);
        assertThat(systemSettingsService.getDefaultLanguage()).isEqualTo(LanguageCode.SW);
        AuthRegistrationResponse swSeeker = seekerRegistrationService.registerWithVerification(
                new RegisterSeekerRequest("Swahili Seeker", phone(), email(), "Pass12345!", null, true));
        assertThat(languagePreferenceRepository.findByUserId(swSeeker.userId()).orElseThrow()
                .getLanguageCode()).isEqualTo(LanguageCode.SW);

        String newPhone = phone();
        var challenge = identityService.requestContactChange(phone, newPhone, null);
        assertThat(userRepository.findByPhone(phone).orElseThrow().getPhone()).isEqualTo(phone);
        identityService.verifyContactChange(phone, challenge.challengeId(), challenge.code());
        assertThat(userRepository.findByPhone(newPhone)).isPresent();
        assertThatThrownBy(() -> identityService.requestContactChange(newPhone, ADMIN, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already registered");
    }

    @Test
    void institutionalRequestsAreOfficerDecidedReschedulableAndReported() {
        String managerPhone = phone();
        String officerPhone = phone();
        Long institutionId = registerAndApproveInstitution(managerPhone);
        Long officerId = staffService.createStaff(institutionId, managerPhone,
                new CreateStaffRequest("Officer", officerPhone, null, "Pass12345!",
                        LanguageCode.EN, AccountType.APPOINTMENT_OFFICER));
        staffService.verify(institutionId, officerId, managerPhone);

        Long providerId = createAndVerifyInstitutionalProvider(institutionId, managerPhone, 0, 24);
        Provider provider = providerRepository.findById(providerId).orElseThrow();
        assertThat(provider.isPublished()).isTrue();

        Long slotId = availabilityService.createSlot(providerId, managerPhone,
                slot(LocalDateTime.now().plusDays(2), 1));
        String seekerPhone = registerVerifiedSeeker();
        Long bookingId = bookingService.requestBooking(seekerPhone, new CreateBookingRequest(
                providerId, slotId, "Please confirm", true, "Child One", "Child"));

        BookingView pending = bookingService.getBooking(bookingId, seekerPhone);
        assertThat(pending.status()).isEqualTo(BookingStatus.PENDING);
        assertThat(pending.attendeeName()).isEqualTo("Child One");
        assertThat(pending.relationship()).isEqualTo("Child");
        assertThat(pending.seekerPhone()).isEqualTo(seekerPhone);
        assertThat(bookingService.institutionBookings(officerPhone)).hasSize(1);
        assertThat(publicCatalogService.listAvailableSlots(providerId)).isEmpty();

        assertThatThrownBy(() -> bookingService.approve(bookingId, seekerPhone, null))
                .isInstanceOf(ForbiddenException.class);
        bookingService.approve(bookingId, officerPhone, new DecisionRequest(null));
        assertThat(bookingRepository.findById(bookingId).orElseThrow().getStatus())
                .isEqualTo(BookingStatus.CONFIRMED);
        assertThat(notificationRepository.findByUserIdOrderByCreatedAtDesc(
                userRepository.findByPhone(seekerPhone).orElseThrow().getId()))
                .anyMatch(notification -> "booking.approved".equals(notification.getMessageKey()));

        Long newSlotId = availabilityService.createSlot(providerId, managerPhone,
                slot(LocalDateTime.now().plusDays(3), 1));
        Long rescheduleId = bookingService.requestReschedule(bookingId, seekerPhone,
                com.example.conversationHub.web.dto.RescheduleRequestDto.request(newSlotId, "Earlier day"));
        bookingService.approveReschedule(rescheduleId, officerPhone, null);
        Booking rescheduled = bookingRepository.findById(bookingId).orElseThrow();
        assertThat(rescheduled.getSlot().getId()).isEqualTo(newSlotId);
        assertThat(slotRepository.findById(slotId).orElseThrow().getBookedCount()).isZero();

        Long withdrawSlotId = availabilityService.createSlot(providerId, managerPhone,
                slot(LocalDateTime.now().plusDays(4), 2));
        Long withdrawnId = bookingService.requestBooking(seekerPhone,
                new CreateBookingRequest(providerId, withdrawSlotId, null, false, null, null));
        bookingService.withdraw(withdrawnId, seekerPhone);
        assertThat(bookingRepository.findById(withdrawnId).orElseThrow().getStatus())
                .isEqualTo(BookingStatus.WITHDRAWN);
        assertThat(slotRepository.findById(withdrawSlotId).orElseThrow().getBookedCount()).isZero();

        var stats = reportingService.institutionStats(institutionId, officerPhone);
        assertThat(stats.approvalCount()).isGreaterThanOrEqualTo(1);
        assertThat(stats.requestCount()).isEqualTo(2);
        assertThat(reportingService.platformStats(ADMIN).bookingCount()).isGreaterThanOrEqualTo(2);
        assertThat(reportingService.platformBookingsCsv(ADMIN)).contains(seekerPhone);
    }

    @Test
    void independentProviderOwnsDecisionsAndLeadTimeIsEnforced() {
        ServiceEntity service = serviceRepository
                .findByCategoryAndServiceType(ServiceCategory.COUNSELLING_COMFORTING, "Counselling")
                .orElseThrow();
        String providerPhone = phone();
        Long providerId = providerManagementService.registerIndependentProvider(
                independentProviderRequest(providerPhone, service.getId(), 24));
        providerManagementService.verifyIndependentProvider(providerId, ADMIN);
        Provider provider = providerRepository.findById(providerId).orElseThrow();
        assertThat(provider.isPublished()).isTrue();
        assertThat(provider.getIdentityDetails()).isNotBlank();

        Long leadTimeSlot = availabilityService.createSlot(providerId, providerPhone,
                slot(LocalDateTime.now().plusHours(2), 1));
        String seekerPhone = registerVerifiedSeeker();
        assertThatThrownBy(() -> bookingService.requestBooking(seekerPhone,
                new CreateBookingRequest(providerId, leadTimeSlot, null, false, null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("lead time");

        Long validSlot = availabilityService.createSlot(providerId, providerPhone,
                slot(LocalDateTime.now().plusDays(2), 1));
        Long bookingId = bookingService.requestBooking(seekerPhone,
                new CreateBookingRequest(providerId, validSlot, "Need support", false, null, null));
        bookingService.approve(bookingId, providerPhone, null);
        assertThat(bookingRepository.findById(bookingId).orElseThrow().getStatus())
                .isEqualTo(BookingStatus.CONFIRMED);

        ProviderDetailView detail = publicCatalogService.getProviderDetail(providerId);
        assertThat(detail.name()).isEqualTo("Counsellor");
        assertThat(detail.verificationStatus()).isEqualTo(provider.getVerificationStatus());
        assertThat(detail.affiliation()).isNull();
        assertThat(detail.hasAvailableSlots()).isTrue();

        providerProfileService.updateOwnProfile(providerPhone,
                new ProviderUpdateRequest("Renamed Counsellor", null, null, null, null, null,
                        null, null, null, null, null, null, null));
        Provider reloaded = providerRepository.findById(providerId).orElseThrow();
        assertThat(reloaded.getName()).isEqualTo("Counsellor"); // published snapshot unchanged
        assertThat(reloaded.isPendingReverification()).isTrue();
        providerManagementService.verifyIndependentProvider(providerId, ADMIN);
        assertThat(providerRepository.findById(providerId).orElseThrow().getName())
                .isEqualTo("Renamed Counsellor");
    }

    @Test
    void expiredRequestsReleaseCapacityAndNoShowsTemporarilyRestrictSeekers() {
        String managerPhone = phone();
        Long institutionId = registerAndApproveInstitution(managerPhone);
        Long providerId = createAndVerifyInstitutionalProvider(institutionId, managerPhone, 0, 24);
        String seekerPhone = registerVerifiedSeeker();

        Long expiringSlotId = availabilityService.createSlot(providerId, managerPhone,
                slot(LocalDateTime.now().plusDays(1), 1));
        Long expiringBookingId = bookingService.requestBooking(seekerPhone,
                new CreateBookingRequest(providerId, expiringSlotId, null, false, null, null));
        Booking expiring = bookingRepository.findById(expiringBookingId).orElseThrow();
        expiring.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        bookingRepository.save(expiring);
        bookingRepository.flush();

        assertThat(bookingService.expirePendingBookings()).isGreaterThanOrEqualTo(1);
        assertThat(bookingRepository.findById(expiringBookingId).orElseThrow().getStatus())
                .isEqualTo(BookingStatus.EXPIRED);
        assertThat(slotRepository.findById(expiringSlotId).orElseThrow().getBookedCount()).isZero();

        Long attendedSlotId = availabilityService.createSlot(providerId, managerPhone,
                slot(LocalDateTime.now().plusDays(2), 1));
        Long confirmedBookingId = bookingService.requestBooking(seekerPhone,
                new CreateBookingRequest(providerId, attendedSlotId, null, false, null, null));
        bookingService.approve(confirmedBookingId, managerPhone, null);
        AvailabilitySlot attendedSlot = slotRepository.findById(attendedSlotId).orElseThrow();
        attendedSlot.setStartTime(LocalDateTime.now().minusHours(2));
        attendedSlot.setEndTime(LocalDateTime.now().minusHours(1));
        slotRepository.save(attendedSlot);

        bookingService.markNoShow(confirmedBookingId, managerPhone);
        assertThat(bookingRepository.findById(confirmedBookingId).orElseThrow().getStatus())
                .isEqualTo(BookingStatus.NO_SHOW);
        assertThat(userRepository.findByPhone(seekerPhone).orElseThrow().getBookingRestrictedUntil())
                .isAfter(LocalDateTime.now());

        Long laterSlotId = availabilityService.createSlot(providerId, managerPhone,
                slot(LocalDateTime.now().plusDays(3), 1));
        assertThatThrownBy(() -> bookingService.requestBooking(seekerPhone,
                new CreateBookingRequest(providerId, laterSlotId, null, false, null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("restricted");
    }

    @Test
    void guestCanBrowseCatalogueButProtectedEndpointsRequireRole() {
        Long institutionId = registerAndApproveInstitution(phone());
        assertThat(publicCatalogService.listApprovedInstitutions())
                .anyMatch(institution -> institution.id().equals(institutionId));
        assertThat(publicCatalogService.listServices(null)).isNotEmpty();
        assertThat(publicCatalogService.listVerifiedProviders(null)).isEmpty();
        RegisterInstitutionRequest guestRequest = new RegisterInstitutionRequest(institutionRequest(),
                "Guest Manager", phone(), email(), "Pass12345!", LanguageCode.EN);
        assertThatThrownBy(() -> institutionService.registerInstitution(
                guestRequest, registerVerifiedSeeker()))
                .isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> reportingService.platformStats(registerVerifiedSeeker()))
                .isInstanceOf(ForbiddenException.class);
    }

    private Long registerAndApproveInstitution(String managerPhone) {
        Long id = institutionService.registerInstitution(
                new RegisterInstitutionRequest(institutionRequest(), "Manager", managerPhone,
                        email(), "Pass12345!", LanguageCode.EN), ADMIN);
        institutionService.approve(id, ADMIN);
        return id;
    }

    private Long createAndVerifyInstitutionalProvider(Long institutionId, String managerPhone,
                                                       int leadTimeHours, int cutoffHours) {
        ServiceEntity service = serviceRepository
                .findByCategoryAndServiceType(ServiceCategory.INSTITUTIONAL, "Institutional appointment")
                .orElseThrow();
        Long providerId = providerManagementService.createInstitutionalProvider(institutionId,
                managerPhone, new CreateInstitutionalProviderRequest("Leader", phone(), email(),
                        "Pass12345!", LanguageCode.EN, service.getId(), "Leadership",
                        ServiceMode.IN_PERSON, 48, "Cancel before the day.", "Dar es Salaam",
                        "National ID", "-degree", "ref-1", leadTimeHours, cutoffHours,
                        "No-show rule", 24));
        providerManagementService.verifyInstitutionalProvider(institutionId, providerId, managerPhone);
        return providerId;
    }

    private RegisterIndependentProviderRequest independentProviderRequest(String providerPhone,
                                                                           Long serviceId,
                                                                           int leadTimeHours) {
        return new RegisterIndependentProviderRequest("Counsellor", providerPhone, email(),
                "Pass12345!", LanguageCode.EN, serviceId, "Emotional support", ServiceMode.REMOTE,
                48, "Cancel before the day.", "Online", "National ID", "Diploma in counselling",
                "ref-2", leadTimeHours, 24, "No-show rule", 24);
    }

    private InstitutionRequest institutionRequest() {
        return new InstitutionRequest("Test Institution", InstitutionType.GOVERNMENT, "Dar es Salaam",
                phone(), email(), "Sample address");
    }

    private SlotRequest slot(LocalDateTime start, int capacity) {
        return new SlotRequest(start, start.plusMinutes(30), capacity, false);
    }

    private String registerVerifiedSeeker() {
        String phone = phone();
        AuthRegistrationResponse registration = seekerRegistrationService.registerWithVerification(
                new RegisterSeekerRequest("Seeker", phone, email(), "Pass12345!",
                        LanguageCode.EN, true));
        identityService.verifyChallenge(registration.challengeId(), registration.code());
        return phone;
    }

    private static String phone() {
        return "+2557" + String.format("%07d", 1000000 + SEQUENCE.incrementAndGet());
    }

    private static String email() {
        return "user" + SEQUENCE.incrementAndGet() + "@example.test";
    }

    private static String basicAuth(String username, String password) {
        String credentials = username + ":" + password;
        return "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
    }
}
