package org.arghyam.jalsoochak.user.repository.records;

import org.arghyam.jalsoochak.user.enums.OtpType;

import java.time.Instant;

/**
 * Immutable projection of a row from {@code common_schema.otp_table}.
 */
public record OtpRow(
        Long id,
        String encryptedOtp,
        Integer tenantId,
        Integer userId,
        OtpType otpType,
        int attemptCount,
        Instant createdAt,
        Instant expiresAt,
        Instant usedAt
) {}
