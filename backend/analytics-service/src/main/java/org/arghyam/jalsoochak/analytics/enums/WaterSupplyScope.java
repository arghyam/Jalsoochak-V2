package org.arghyam.jalsoochak.analytics.enums;

public enum WaterSupplyScope {
    SCHEME,
    CHILD;

    public static WaterSupplyScope fromValue(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("scope is required and must be one of: scheme, child");
        }
        return switch (value.trim().toLowerCase()) {
            case "scheme" -> SCHEME;
            case "child" -> CHILD;
            default -> throw new IllegalArgumentException("Unsupported scope: " + value + ". Use scheme or child");
        };
    }
}
