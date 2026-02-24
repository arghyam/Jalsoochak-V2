package com.example.telemetry.repository;

public record TelemetryOperatorWithSchema(
        String schemaName,
        TelemetryOperator operator
) {
}
