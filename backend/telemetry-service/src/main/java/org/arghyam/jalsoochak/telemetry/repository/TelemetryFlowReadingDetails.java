package org.arghyam.jalsoochak.telemetry.repository;

import java.math.BigDecimal;

public record TelemetryFlowReadingDetails(
        Long id,
        String correlationId,
        Long createdBy,
        BigDecimal extractedReading,
        BigDecimal confirmedReading
) {
}

