package org.arghyam.jalsoochak.tenant.enums;

/**
 * Represents the configuration status of a single configuration key for a tenant.
 * Used in {@code TenantConfigStatusResponseDTO.ConfigEntry} to enforce allowed values.
 */
public enum ConfigStatusEnum {
    CONFIGURED,
    PENDING
}
