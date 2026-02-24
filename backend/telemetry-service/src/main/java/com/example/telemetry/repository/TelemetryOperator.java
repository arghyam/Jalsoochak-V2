package com.example.telemetry.repository;

public record TelemetryOperator(
        Long id,
        Integer tenantId,
        String title,
        String email,
        String phoneNumber,
        Integer languageId
) {
}
