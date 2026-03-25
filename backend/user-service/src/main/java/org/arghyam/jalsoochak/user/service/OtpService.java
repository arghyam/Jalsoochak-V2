package org.arghyam.jalsoochak.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.arghyam.jalsoochak.user.config.properties.OtpProperties;
import org.arghyam.jalsoochak.user.enums.OtpType;
import org.arghyam.jalsoochak.user.exceptions.BadRequestException;
import org.arghyam.jalsoochak.user.repository.OtpRepository;
import org.arghyam.jalsoochak.user.repository.records.OtpRow;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Core OTP lifecycle service: generation, storage, and verification.
 *
 * <p>OTP values are stored AES-256-GCM encrypted (via {@link PiiEncryptionService}).
 * They are never persisted in plaintext.
 *
 * <p>Callers are responsible for wrapping these calls in a transaction so that
 * revoke + insert (or find + mark-used) are atomic.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OtpService {

    private final OtpRepository otpRepository;
    private final PiiEncryptionService piiEncryptionService;
    private final OtpProperties otpProperties;

    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * Generates a new OTP for {@code userId/tenantId/otpType}, persists it encrypted,
     * and returns the raw plaintext OTP for delivery.
     *
     * <p>Cooldown check: if an active OTP exists and was created within
     * {@code otp.cooldown-seconds}, throws {@link BadRequestException}.
     * Otherwise revokes any existing active OTP and inserts a fresh one.
     *
     * @return raw (plaintext) OTP string — deliver to the user, never store
     */
    public String requestOtp(Long userId, Integer tenantId, OtpType otpType) {
        // Enforce cooldown to prevent OTP flooding
        otpRepository.findActiveOtp(userId, tenantId, otpType).ifPresent(existing -> {
            long secondsElapsed = ChronoUnit.SECONDS.between(existing.createdAt(), Instant.now());
            if (secondsElapsed < otpProperties.cooldownSeconds()) {
                long remaining = otpProperties.cooldownSeconds() - secondsElapsed;
                throw new BadRequestException(
                        "Please wait " + remaining + " second(s) before requesting a new OTP");
            }
        });

        // Revoke any stale active OTP before inserting the new one
        otpRepository.revokeActiveOtp(userId, tenantId, otpType);

        String rawOtp = generateOtp();
        String encryptedOtp = piiEncryptionService.encrypt(rawOtp);
        Instant expiresAt = Instant.now().plus(otpProperties.expiryMinutes(), ChronoUnit.MINUTES);

        otpRepository.insertOtp(userId, tenantId, otpType, encryptedOtp, expiresAt);

        log.debug("OTP generated for userId={} tenantId={} type={}", userId, tenantId, otpType);
        return rawOtp;
    }

    /**
     * Verifies the supplied {@code rawOtp} against the stored active OTP.
     *
     * <p>On mismatch: increments attempt_count. If attempt_count reaches
     * {@code otp.max-attempts}, the OTP is effectively locked (further calls will fail
     * with the same error until the OTP expires).
     *
     * <p>On match: marks the OTP as used. It cannot be replayed.
     *
     * @throws BadRequestException if there is no active OTP, max attempts exceeded, or mismatch
     */
    public void verifyOtp(Long userId, Integer tenantId, OtpType otpType, String rawOtp) {
        OtpRow otpRow = otpRepository.findActiveOtp(userId, tenantId, otpType)
                .orElseThrow(() -> new BadRequestException("Invalid or expired OTP"));

        if (otpRow.attemptCount() >= otpProperties.maxAttempts()) {
            throw new BadRequestException("Maximum OTP attempts exceeded. Please request a new OTP");
        }

        String storedOtp = piiEncryptionService.decrypt(otpRow.encryptedOtp());
        if (!storedOtp.equals(rawOtp)) {
            otpRepository.incrementAttemptCount(otpRow.id());
            log.debug("OTP mismatch for userId={} tenantId={} type={} attempts={}",
                    userId, tenantId, otpType, otpRow.attemptCount() + 1);
            throw new BadRequestException("Invalid or expired OTP");
        }

        boolean consumed = otpRepository.markUsed(otpRow.id());
        if (!consumed) {
            throw new BadRequestException("Invalid or expired OTP");
        }
        log.debug("OTP verified for userId={} tenantId={} type={}", userId, tenantId, otpType);
    }

    private String generateOtp() {
        StringBuilder sb = new StringBuilder(otpProperties.otpLength());
        for (int i = 0; i < otpProperties.otpLength(); i++) {
            sb.append(secureRandom.nextInt(10));
        }
        return sb.toString();
    }
}
