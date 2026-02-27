package org.arghyam.jalsoochak.telemetry.repository;

public record TelemetryOperatorWithSchema(
        String schemaName,
        TelemetryOperator operator
) {
}
