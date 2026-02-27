package org.arghyam.jalsoochak.telemetry.repository;

public record TelemetryReadingRecord(
        Long id,
        String correlationId,
        Long createdBy
) {
}
