package com.example.conversationHub.web.controller;

import com.example.conversationHub.service.InstitutionService;
import com.example.conversationHub.service.ProviderManagementService;
import com.example.conversationHub.service.SeekerRegistrationService;
import com.example.conversationHub.web.dto.ApiResponse;
import com.example.conversationHub.web.dto.RegisterIndependentProviderRequest;
import com.example.conversationHub.web.dto.RegisterInstitutionRequest;
import com.example.conversationHub.web.dto.RegisterSeekerRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class RegistrationController {

    private final SeekerRegistrationService registrationService;
    private final InstitutionService institutionService;
    private final ProviderManagementService providerManagementService;

    public RegistrationController(SeekerRegistrationService registrationService,
                                  InstitutionService institutionService,
                                  ProviderManagementService providerManagementService) {
        this.registrationService = registrationService;
        this.institutionService = institutionService;
        this.providerManagementService = providerManagementService;
    }

    @PostMapping("/register/seeker")
    public ResponseEntity<ApiResponse> registerSeeker(@Valid @RequestBody RegisterSeekerRequest request) {
        Long id = registrationService.registerSeeker(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Seeker registered", id));
    }

    @PostMapping("/register/institution")
    public ResponseEntity<ApiResponse> registerInstitution(
            @Valid @RequestBody RegisterInstitutionRequest request) {
        Long id = institutionService.registerInstitution(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Institution submitted for approval", id));
    }

    @PostMapping("/register/provider/independent")
    public ResponseEntity<ApiResponse> registerIndependentProvider(
            @Valid @RequestBody RegisterIndependentProviderRequest request) {
        Long id = providerManagementService.registerIndependentProvider(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Independent provider submitted for verification", id));
    }
}
