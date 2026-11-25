package com.example.conversationHub.web.controller;

import com.example.conversationHub.domain.enums.BookingStatus;
import com.example.conversationHub.domain.enums.InstitutionStatus;
import com.example.conversationHub.domain.enums.LanguageCode;
import com.example.conversationHub.domain.enums.ServiceMode;
import com.example.conversationHub.service.AdminQueryService;
import com.example.conversationHub.service.AvailabilityService;
import com.example.conversationHub.service.BookingService;
import com.example.conversationHub.service.InstitutionService;
import com.example.conversationHub.service.NotificationService;
import com.example.conversationHub.service.ProfileService;
import com.example.conversationHub.service.ProviderManagementService;
import com.example.conversationHub.service.PublicCatalogService;
import com.example.conversationHub.service.SeekerRegistrationService;
import com.example.conversationHub.service.StaffService;
import com.example.conversationHub.web.dto.CreateBookingRequest;
import com.example.conversationHub.web.dto.RegisterIndependentProviderRequest;
import com.example.conversationHub.web.dto.RegisterInstitutionRequest;
import com.example.conversationHub.web.dto.RegisterSeekerRequest;
import com.example.conversationHub.web.dto.SlotRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class PageController {

    private final PublicCatalogService catalogService;
    private final ProfileService profileService;
    private final BookingService bookingService;
    private final NotificationService notificationService;
    private final InstitutionService institutionService;
    private final ProviderManagementService providerManagementService;
    private final StaffService staffService;
    private final AvailabilityService availabilityService;
    private final AdminQueryService adminQueryService;
    private final SeekerRegistrationService seekerRegistrationService;

    public PageController(PublicCatalogService catalogService,
                          ProfileService profileService,
                          BookingService bookingService,
                          NotificationService notificationService,
                          InstitutionService institutionService,
                          ProviderManagementService providerManagementService,
                          StaffService staffService,
                          AvailabilityService availabilityService,
                          AdminQueryService adminQueryService,
                          SeekerRegistrationService seekerRegistrationService) {
        this.catalogService = catalogService;
        this.profileService = profileService;
        this.bookingService = bookingService;
        this.notificationService = notificationService;
        this.institutionService = institutionService;
        this.providerManagementService = providerManagementService;
        this.staffService = staffService;
        this.availabilityService = availabilityService;
        this.adminQueryService = adminQueryService;
        this.seekerRegistrationService = seekerRegistrationService;
    }

    @GetMapping("/")
    public String home() {
        return "index";
    }

    @GetMapping("/services")
    public String services(Model model) {
        model.addAttribute("services", catalogService.listServices());
        return "services";
    }

    @GetMapping("/providers")
    public String providers(Model model) {
        model.addAttribute("providers", catalogService.listVerifiedProviders());
        model.addAttribute("institutions", catalogService.listApprovedInstitutions());
        return "providers";
    }

    @GetMapping("/register")
    public String register(Model model) {
        if (!model.containsAttribute("seeker")) {
            model.addAttribute("seeker", new RegisterSeekerRequest("", "", "", "", LanguageCode.EN));
        }
        return "auth/register";
    }

    @PostMapping("/register/seeker")
    public String registerSeeker(@Valid @ModelAttribute("seeker") RegisterSeekerRequest request,
                                 BindingResult bindingResult,
                                 RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.seeker", bindingResult);
            redirectAttributes.addFlashAttribute("seeker", request);
            redirectAttributes.addFlashAttribute("error", "Please correct the highlighted fields.");
            return "redirect:/register";
        }
        try {
            seekerRegistrationService.registerSeeker(request);
            redirectAttributes.addFlashAttribute("success", "Account created. You can now log in.");
            return "redirect:/login";
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
            redirectAttributes.addFlashAttribute("seeker", request);
            return "redirect:/register";
        }
    }

    @GetMapping("/login")
    public String login() {
        return "auth/login";
    }

    @GetMapping("/dashboard")
    public String dashboard(Authentication authentication, Model model) {
        var me = profileService.me(authentication.getName());
        model.addAttribute("me", me);
        model.addAttribute("notifications", notificationService.listMine(authentication.getName()));
        model.addAttribute("unreadCount", notificationService.unreadCount(authentication.getName()));

        switch (me.accountType()) {
            case SEEKER -> model.addAttribute("bookings", bookingService.myBookings(authentication.getName()));
            case INDEPENDENT_PROVIDER, INSTITUTIONAL_PROVIDER ->
                    model.addAttribute("bookings", bookingService.providerBookings(authentication.getName()));
            case CENTER_MANAGER, APPOINTMENT_OFFICER ->
                    model.addAttribute("bookings", bookingService.institutionBookings(authentication.getName()));
            case MAIN_ADMIN -> {
                model.addAttribute("pendingInstitutions", institutionService.listPending());
                model.addAttribute("pendingProviders", providerManagementService.listPendingIndependentProviders(authentication.getName()));
                model.addAttribute("bookings", adminQueryService.allBookings());
                model.addAttribute("auditLogs", adminQueryService.recentAuditLogs());
            }
        }
        return "dashboard/index";
    }

    @GetMapping("/book/{providerId}")
    public String book(@PathVariable Long providerId, Model model) {
        model.addAttribute("providerId", providerId);
        model.addAttribute("slots", catalogService.listAvailableSlots(providerId));
        model.addAttribute("booking", new CreateBookingRequest(providerId, null, "", false, "", ""));
        return "booking";
    }

    @PostMapping("/book")
    public String submitBooking(Authentication authentication,
                                @Valid @ModelAttribute("booking") CreateBookingRequest request,
                                BindingResult bindingResult,
                                RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("error", "Please check the booking details.");
            return "redirect:/book/" + request.providerId();
        }
        try {
            bookingService.requestBooking(authentication.getName(), request);
            redirectAttributes.addFlashAttribute("success", "Booking request submitted.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
            return "redirect:/book/" + request.providerId();
        }
        return "redirect:/dashboard";
    }

    @PostMapping("/dashboard/bookings/{id}/approve")
    public String approveBooking(@PathVariable Long id, Authentication auth, RedirectAttributes ra) {
        try { bookingService.approve(id, auth.getName(), null); ra.addFlashAttribute("success", "Booking approved."); }
        catch (Exception e) { ra.addFlashAttribute("error", e.getMessage()); }
        return "redirect:/dashboard";
    }

    @PostMapping("/dashboard/bookings/{id}/reject")
    public String rejectBooking(@PathVariable Long id, Authentication auth, RedirectAttributes ra) {
        try { bookingService.reject(id, auth.getName(), null); ra.addFlashAttribute("success", "Booking rejected."); }
        catch (Exception e) { ra.addFlashAttribute("error", e.getMessage()); }
        return "redirect:/dashboard";
    }

    @PostMapping("/dashboard/bookings/{id}/cancel")
    public String cancelBooking(@PathVariable Long id, Authentication auth, RedirectAttributes ra) {
        try { bookingService.cancelBySeeker(id, auth.getName(), null); ra.addFlashAttribute("success", "Booking cancelled."); }
        catch (Exception e) { ra.addFlashAttribute("error", e.getMessage()); }
        return "redirect:/dashboard";
    }

    @PostMapping("/dashboard/bookings/{id}/complete")
    public String completeBooking(@PathVariable Long id, Authentication auth, RedirectAttributes ra) {
        try { bookingService.complete(id, auth.getName()); ra.addFlashAttribute("success", "Booking completed."); }
        catch (Exception e) { ra.addFlashAttribute("error", e.getMessage()); }
        return "redirect:/dashboard";
    }

    @PostMapping("/dashboard/admin/institutions/{id}/approve")
    public String approveInstitution(@PathVariable Long id, Authentication auth, RedirectAttributes ra) {
        try { institutionService.approve(id, auth.getName()); ra.addFlashAttribute("success", "Institution approved."); }
        catch (Exception e) { ra.addFlashAttribute("error", e.getMessage()); }
        return "redirect:/dashboard";
    }
}
