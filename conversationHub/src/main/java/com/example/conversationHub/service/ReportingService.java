package com.example.conversationHub.service;

import com.example.conversationHub.domain.AppUser;
import com.example.conversationHub.domain.AvailabilitySlot;
import com.example.conversationHub.domain.Booking;
import com.example.conversationHub.domain.Institution;
import com.example.conversationHub.domain.Provider;
import com.example.conversationHub.domain.ServiceEntity;
import com.example.conversationHub.domain.enums.AccountType;
import com.example.conversationHub.domain.enums.BookingStatus;
import com.example.conversationHub.repository.BookingRepository;
import com.example.conversationHub.repository.InstitutionRepository;
import com.example.conversationHub.repository.ProviderRepository;
import com.example.conversationHub.web.dto.BookingReportRow;
import com.example.conversationHub.web.dto.InstitutionStatsView;
import com.example.conversationHub.web.dto.PlatformStatsView;
import com.example.conversationHub.web.exception.ForbiddenException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@Transactional(readOnly = true)
public class ReportingService {

    private static final String CSV_HEADER = String.join(",",
            "id", "status", "institution", "provider", "service category", "service type",
            "seeker name", "seeker phone", "seeker email", "booking for other",
            "attendee name", "relationship", "start time", "end time", "submitted at",
            "decided at", "cancellation rule", "message");

    private final InstitutionRepository institutionRepository;
    private final ProviderRepository providerRepository;
    private final BookingRepository bookingRepository;
    private final CurrentUserService currentUserService;

    public ReportingService(InstitutionRepository institutionRepository,
                            ProviderRepository providerRepository,
                            BookingRepository bookingRepository,
                            CurrentUserService currentUserService) {
        this.institutionRepository = institutionRepository;
        this.providerRepository = providerRepository;
        this.bookingRepository = bookingRepository;
        this.currentUserService = currentUserService;
    }

    public PlatformStatsView platformStats(String username) {
        requireAdmin(username);

        Map<String, Long> institutionsByStatus = new LinkedHashMap<>();
        for (Institution institution : institutionRepository.findAll()) {
            increment(institutionsByStatus,
                    institution.getStatus() == null ? "UNKNOWN" : institution.getStatus().name());
        }

        Map<String, Long> providersByCategory = new LinkedHashMap<>();
        for (Provider provider : providerRepository.findAll()) {
            increment(providersByCategory, categoryOf(provider.getService()));
        }

        Map<String, Long> bookingsByCategory = new LinkedHashMap<>();
        Map<String, Long> bookingsByStatus = new LinkedHashMap<>();
        Map<String, Map<String, Long>> byCategoryAndStatus = new LinkedHashMap<>();
        for (Booking booking : bookingRepository.findAll()) {
            String category = categoryOf(booking.getService());
            String status = statusOf(booking);
            increment(bookingsByCategory, category);
            increment(bookingsByStatus, status);
            byCategoryAndStatus.computeIfAbsent(category, key -> new LinkedHashMap<>());
            increment(byCategoryAndStatus.get(category), status);
        }

        return new PlatformStatsView(
                institutionRepository.count(),
                providerRepository.count(),
                bookingRepository.count(),
                institutionsByStatus,
                providersByCategory,
                bookingsByCategory,
                bookingsByStatus,
                byCategoryAndStatus
        );
    }

    public InstitutionStatsView institutionStats(Long institutionId, String username) {
        requireInstitutionViewer(institutionId, username);
        Institution institution = institutionRepository.findById(institutionId)
                .orElseThrow(() -> new IllegalArgumentException("Institution not found"));
        List<Booking> bookings = bookingRepository.findByProvider_Institution_Id(institutionId);

        long approvals = 0;
        long rejections = 0;
        long noShows = 0;
        long pending = 0;
        Map<String, Long> byCategory = new LinkedHashMap<>();
        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (Booking booking : bookings) {
            BookingStatus status = booking.getStatus();
            if (status == BookingStatus.CONFIRMED || status == BookingStatus.COMPLETED
                    || status == BookingStatus.NO_SHOW) {
                approvals++;
            }
            if (status == BookingStatus.REJECTED) {
                rejections++;
            }
            if (status == BookingStatus.NO_SHOW) {
                noShows++;
            }
            if (status == BookingStatus.PENDING) {
                pending++;
            }
            increment(byCategory, categoryOf(booking.getService()));
            increment(byStatus, statusOf(booking));
        }

        return new InstitutionStatsView(
                institution.getId(),
                institution.getName(),
                bookings.size(),
                approvals,
                rejections,
                noShows,
                pending,
                byCategory,
                byStatus
        );
    }

    public List<BookingReportRow> platformBookingRows(String username) {
        requireAdmin(username);
        return bookingRepository.findAll().stream().map(this::toRow).toList();
    }

    public List<BookingReportRow> institutionBookingRows(Long institutionId, String username) {
        requireInstitutionViewer(institutionId, username);
        return bookingRepository.findByProvider_Institution_Id(institutionId).stream()
                .map(this::toRow)
                .toList();
    }

    public String platformBookingsCsv(String username) {
        return toCsv(platformBookingRows(username));
    }

    public String institutionBookingsCsv(Long institutionId, String username) {
        return toCsv(institutionBookingRows(institutionId, username));
    }

    private void requireAdmin(String username) {
        AppUser actor = currentUserService.require(username);
        if (actor.getAccountType() != AccountType.MAIN_ADMIN) {
            throw new ForbiddenException("Main administrator access is required");
        }
    }

    private void requireInstitutionViewer(Long institutionId, String username) {
        AppUser actor = currentUserService.require(username);
        if (actor.getAccountType() == AccountType.MAIN_ADMIN) {
            return;
        }
        if (institutionId == null) {
            throw new IllegalArgumentException("Institution id is required");
        }
        if ((actor.getAccountType() == AccountType.CENTER_MANAGER
                || actor.getAccountType() == AccountType.APPOINTMENT_OFFICER)
                && actor.getInstitution() != null
                && Objects.equals(actor.getInstitution().getId(), institutionId)) {
            return;
        }
        throw new ForbiddenException("You can only view reports for your own institution");
    }

    private BookingReportRow toRow(Booking booking) {
        Provider provider = booking.getProvider();
        ServiceEntity service = booking.getService();
        AppUser seeker = booking.getSeeker();
        AvailabilitySlot slot = booking.getSlot();
        return new BookingReportRow(
                booking.getId(),
                statusOf(booking),
                provider == null || provider.getInstitution() == null
                        ? null : provider.getInstitution().getName(),
                provider == null ? null : provider.getName(),
                categoryOf(service),
                service == null ? null : service.getServiceType(),
                seeker == null ? null : seeker.getName(),
                seeker == null ? null : seeker.getPhone(),
                seeker == null ? null : seeker.getEmail(),
                booking.isBookingForOther(),
                booking.getAttendeeName(),
                booking.getRelationship(),
                slot == null ? null : slot.getStartTime(),
                slot == null ? null : slot.getEndTime(),
                booking.getSubmittedAt(),
                booking.getDecidedAt(),
                booking.getCancellationRule(),
                booking.getMessage()
        );
    }

    private String categoryOf(ServiceEntity service) {
        return service == null || service.getCategory() == null ? "UNKNOWN" : service.getCategory().name();
    }

    private String statusOf(Booking booking) {
        return booking.getStatus() == null ? "UNKNOWN" : booking.getStatus().name();
    }

    private void increment(Map<String, Long> counts, String key) {
        counts.merge(key, 1L, Long::sum);
    }

    private String toCsv(List<BookingReportRow> rows) {
        StringBuilder csv = new StringBuilder(CSV_HEADER);
        for (BookingReportRow row : rows) {
            List<Object> fields = new ArrayList<>();
            fields.add(row.id());
            fields.add(row.status());
            fields.add(row.institutionName());
            fields.add(row.providerName());
            fields.add(row.serviceCategory());
            fields.add(row.serviceType());
            fields.add(row.seekerName());
            fields.add(row.seekerPhone());
            fields.add(row.seekerEmail());
            fields.add(row.bookingForOther());
            fields.add(row.attendeeName());
            fields.add(row.relationship());
            fields.add(row.startTime());
            fields.add(row.endTime());
            fields.add(row.submittedAt());
            fields.add(row.decidedAt());
            fields.add(row.cancellationRule());
            fields.add(row.message());
            csv.append('\n');
            for (int i = 0; i < fields.size(); i++) {
                if (i > 0) {
                    csv.append(',');
                }
                csv.append(escape(fields.get(i)));
            }
        }
        return csv.toString();
    }

    private String escape(Object value) {
        String text = value == null ? "" : value.toString();
        return '"' + text.replace("\"", "\"\"") + '"';
    }
}
