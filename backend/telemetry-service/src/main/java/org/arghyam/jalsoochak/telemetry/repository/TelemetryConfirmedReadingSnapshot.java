package org.arghyam.jalsoochak.telemetry.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TelemetryConfirmedReadingSnapshot(
        BigDecimal confirmedReading,
        LocalDateTime createdAt
) {
}
