package org.arghyam.jalsoochak.user.config.properties;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "password-reset")
public record PasswordResetProperties(@Min(1) int expiryMinutes) {}
