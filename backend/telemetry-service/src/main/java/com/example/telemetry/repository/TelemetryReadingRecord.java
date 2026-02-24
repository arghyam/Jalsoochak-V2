package com.example.telemetry.repository;

public record TelemetryReadingRecord(
        Long id,
        String correlationId,
        Long createdBy
) {
}
