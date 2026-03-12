package org.arghyam.jalsoochak.analytics.enums;

public enum WaterSupplyScope {
    CURRENT,
    CHILD;

    public static WaterSupplyScope fromValue(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("scope is required and must be one of: current, child");
        }
        return switch (value.trim().toLowerCase()) {
            case "current" -> CURRENT;
            case "child" -> CHILD;
            default -> throw new IllegalArgumentException("Unsupported scope: " + value + ". Use current or child");
        };
    }
}
