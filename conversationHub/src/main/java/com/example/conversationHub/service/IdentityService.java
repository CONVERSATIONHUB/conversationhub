package com.example.conversationHub.service;

import com.example.conversationHub.domain.AppUser;
import com.example.conversationHub.domain.PasswordResetToken;
import com.example.conversationHub.domain.VerificationChallenge;
import com.example.conversationHub.domain.enums.VerificationChallengeType;
import com.example.conversationHub.domain.enums.VerificationChannel;
import com.example.conversationHub.repository.AppUserRepository;
import com.example.conversationHub.repository.PasswordResetTokenRepository;
import com.example.conversationHub.repository.VerificationChallengeRepository;
import com.example.conversationHub.web.dto.AuthChallengeResponse;
import com.example.conversationHub.web.dto.AuthPasswordResetResponse;
import com.example.conversationHub.web.dto.ContactChangeRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Local verification and password recovery. Release 1 has no SMS/email transport (NFR-8.4),
 * so generated codes and recovery tokens are returned in the API response while only their
 * hashes are stored in the database.
 */
@Service
@Transactional
public class IdentityService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final VerificationChallengeRepository challengeRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;
    private final CurrentUserService currentUserService;
    private final MessageSource messageSource;
    private final long codeTtlMinutes;
    private final long resetTtlMinutes;
    private final int maxAttempts;

    public IdentityService(VerificationChallengeRepository challengeRepository,
                           PasswordResetTokenRepository tokenRepository,
                           AppUserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           AuditService auditService,
                           CurrentUserService currentUserService,
                           MessageSource messageSource,
                           @Value("${app.verification.code-ttl-minutes:30}") long codeTtlMinutes,
                           @Value("${app.password-reset.ttl-minutes:30}") long resetTtlMinutes,
                           @Value("${app.verification.max-attempts:5}") int maxAttempts) {
        this.challengeRepository = challengeRepository;
        this.tokenRepository = tokenRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
        this.currentUserService = currentUserService;
        this.messageSource = messageSource;
        this.codeTtlMinutes = codeTtlMinutes;
        this.resetTtlMinutes = resetTtlMinutes;
        this.maxAttempts = maxAttempts;
    }

    public AuthChallengeResponse createChallenge(AppUser user, VerificationChannel channel,
                                                 VerificationChallengeType type, String targetValue) {
        String code = String.format("%06d", RANDOM.nextInt(1_000_000));
        VerificationChallenge challenge = new VerificationChallenge();
        challenge.setUser(user);
        challenge.setChannel(channel);
        challenge.setType(type);
        challenge.setTargetValue(targetValue);
        challenge.setCodeHash(passwordEncoder.encode(code));
        challenge.setExpiresAt(LocalDateTime.now().plusMinutes(codeTtlMinutes));
        challenge.setAttempts(0);
        VerificationChallenge saved = challengeRepository.save(challenge);
        return new AuthChallengeResponse(saved.getId(), code, channel.name(), targetValue, user.getId());
    }

    /** Verifies a registration or contact-change challenge and returns the verified user. */
    public AppUser verifyChallenge(Long challengeId, String code) {
        VerificationChallenge challenge = challengeRepository.findById(challengeId)
                .orElseThrow(() -> new IllegalArgumentException(message("auth.invalid_code")));
        AppUser user = challenge.getUser();
        validateChallenge(challenge, code);

        challenge.setConsumedAt(LocalDateTime.now());
        challengeRepository.save(challenge);
        if (!user.isVerified()) {
            user.setVerified(true);
            userRepository.save(user);
            auditService.record(user, "USER_VERIFIED", "APP_USER", user.getId());
        }
        return user;
    }

    /** Convenience alias used by controllers and other services. */
    public AppUser verify(Long challengeId, String code) {
        return verifyChallenge(challengeId, code);
    }

    /** Invalidates the previous challenge and issues a fresh one for the same purpose. */
    public AuthChallengeResponse resendCode(Long challengeId) {
        VerificationChallenge previous = challengeRepository.findById(challengeId)
                .orElseThrow(() -> new IllegalArgumentException(message("auth.invalid_code")));
        if (previous.getAttempts() >= maxAttempts || previous.getConsumedAt() != null) {
            throw new IllegalArgumentException(message("auth.code_expired"));
        }
        previous.setConsumedAt(LocalDateTime.now());
        challengeRepository.save(previous);
        return createChallenge(previous.getUser(), previous.getChannel(), previous.getType(),
                previous.getTargetValue());
    }

    public AuthChallengeResponse resend(Long challengeId) {
        return resendCode(challengeId);
    }

    /** Creates a single-use recovery token; the raw token is returned, a hash is persisted. */
    public AuthPasswordResetResponse forgotPassword(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            throw new IllegalArgumentException(message("auth.invalid_token"));
        }
        String trimmed = identifier.trim();
        AppUser user = userRepository.findByPhone(trimmed)
                .or(() -> userRepository.findByEmail(trimmed))
                .orElseThrow(() -> new IllegalArgumentException(message("auth.invalid_token")));
        if (!user.isVerified() || !user.isActive()) {
            throw new IllegalArgumentException(message("auth.invalid_token"));
        }

        String reference = UUID.randomUUID().toString();
        String rawToken = UUID.randomUUID().toString();
        PasswordResetToken token = new PasswordResetToken();
        token.setUser(user);
        token.setReference(reference);
        token.setTokenHash(passwordEncoder.encode(rawToken));
        token.setExpiresAt(LocalDateTime.now().plusMinutes(resetTtlMinutes));
        token.setAttempts(0);
        PasswordResetToken saved = tokenRepository.save(token);

        return new AuthPasswordResetResponse(reference + ":" + rawToken, saved.getId(),
                resetTtlMinutes * 60);
    }

    public AuthPasswordResetResponse requestPasswordReset(String identifier) {
        return forgotPassword(identifier);
    }

    /** Consumes a recovery token and stores the new (hashed) password. */
    public AppUser resetPassword(String tokenValue, String newPassword) {
        if (tokenValue == null || !tokenValue.contains(":")) {
            throw new IllegalArgumentException(message("auth.invalid_token"));
        }
        int separator = tokenValue.indexOf(':');
        String reference = tokenValue.substring(0, separator);
        String rawToken = tokenValue.substring(separator + 1);
        PasswordResetToken token = tokenRepository.findByReference(reference)
                .orElseThrow(() -> new IllegalArgumentException(message("auth.invalid_token")));
        if (token.getConsumedAt() != null) {
            throw new IllegalArgumentException(message("auth.invalid_token"));
        }
        if (token.getExpiresAt().isBefore(LocalDateTime.now())
                || token.getAttempts() >= maxAttempts) {
            throw new IllegalArgumentException(message("auth.invalid_token"));
        }
        if (!passwordEncoder.matches(rawToken, token.getTokenHash())) {
            token.setAttempts(token.getAttempts() + 1);
            tokenRepository.save(token);
            throw new IllegalArgumentException(message("auth.invalid_token"));
        }

        AppUser user = token.getUser();
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        token.setConsumedAt(LocalDateTime.now());
        tokenRepository.save(token);
        auditService.record(user, "PASSWORD_RESET", "APP_USER", user.getId());
        return user;
    }

    /** Starts a contact change; the code is returned in the response for release 1. */
    public AuthChallengeResponse requestContactChange(String username, ContactChangeRequest request) {
        return requestContactChange(username, request.phone(), request.email());
    }

    public AuthChallengeResponse requestContactChange(String username, String phone, String email) {
        AppUser user = currentUserService.require(username);
        String newPhone = normalize(phone);
        String newEmail = normalize(email);

        VerificationChannel channel;
        String targetValue;
        if (newPhone != null && !newPhone.equals(normalize(user.getPhone()))) {
            assertPhoneAvailable(newPhone, user);
            channel = VerificationChannel.PHONE;
            targetValue = newPhone;
        } else if (newEmail != null && !newEmail.equalsIgnoreCase(normalize(user.getEmail()))) {
            assertEmailAvailable(newEmail, user);
            channel = VerificationChannel.EMAIL;
            targetValue = newEmail;
        } else {
            throw new IllegalArgumentException("No new contact value was supplied");
        }
        return createChallenge(user, channel, VerificationChallengeType.CONTACT_CHANGE, targetValue);
    }

    /** Applies a verified contact change after repeating the uniqueness checks. */
    public AppUser verifyContactChange(String username, Long challengeId, String code) {
        AppUser user = currentUserService.require(username);
        VerificationChallenge challenge = challengeRepository.findById(challengeId)
                .orElseThrow(() -> new IllegalArgumentException(message("auth.invalid_code")));
        if (challenge.getType() != VerificationChallengeType.CONTACT_CHANGE
                || challenge.getUser() == null || !challenge.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException(message("auth.invalid_code"));
        }
        validateChallenge(challenge, code);

        String targetValue = challenge.getTargetValue();
        if (challenge.getChannel() == VerificationChannel.PHONE) {
            assertPhoneAvailable(targetValue, user);
            user.setPhone(targetValue);
        } else {
            assertEmailAvailable(targetValue, user);
            user.setEmail(targetValue);
        }
        userRepository.save(user);
        challenge.setConsumedAt(LocalDateTime.now());
        challengeRepository.save(challenge);
        auditService.record(user, "CONTACT_UPDATED", "APP_USER", user.getId());
        return user;
    }

    @Transactional(readOnly = true)
    public Optional<VerificationChallenge> findChallenge(Long challengeId) {
        return challengeRepository.findById(challengeId);
    }

    private void validateChallenge(VerificationChallenge challenge, String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException(message("auth.invalid_code"));
        }
        if (challenge.getConsumedAt() != null) {
            throw new IllegalArgumentException(message("auth.invalid_code"));
        }
        if (challenge.getExpiresAt().isBefore(LocalDateTime.now())
                || challenge.getAttempts() >= maxAttempts) {
            throw new IllegalArgumentException(message("auth.code_expired"));
        }
        if (!passwordEncoder.matches(code.trim(), challenge.getCodeHash())) {
            challenge.setAttempts(challenge.getAttempts() + 1);
            challengeRepository.save(challenge);
            throw new IllegalArgumentException(message("auth.invalid_code"));
        }
    }

    private void assertPhoneAvailable(String phone, AppUser user) {
        userRepository.findByPhone(phone).ifPresent(existing -> {
            if (!existing.getId().equals(user.getId())) {
                throw new IllegalArgumentException("Phone number is already registered");
            }
        });
    }

    private void assertEmailAvailable(String email, AppUser user) {
        userRepository.findByEmail(email).ifPresent(existing -> {
            if (!existing.getId().equals(user.getId())) {
                throw new IllegalArgumentException("Email address is already registered");
            }
        });
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String message(String key) {
        return messageSource.getMessage(key, null, LocaleContextHolder.getLocale());
    }
}
