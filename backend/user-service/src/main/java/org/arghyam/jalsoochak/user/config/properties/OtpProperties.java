package org.arghyam.jalsoochak.user.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * OTP configuration properties.
 * Bound from {@code otp.*} in application.yml.
 */
@ConfigurationProperties(prefix = "otp")
public record OtpProperties(
        int expiryMinutes,
        int maxAttempts,
        int cooldownSeconds,
        int otpLength,
        String deliveryChannel
) {}
