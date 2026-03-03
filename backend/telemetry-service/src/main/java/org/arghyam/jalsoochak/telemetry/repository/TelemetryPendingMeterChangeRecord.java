package org.arghyam.jalsoochak.telemetry.repository;

import java.math.BigDecimal;

public record TelemetryPendingMeterChangeRecord(
        Long id,
        String correlationId,
        Long createdBy,
        BigDecimal extractedReading
) {
}
