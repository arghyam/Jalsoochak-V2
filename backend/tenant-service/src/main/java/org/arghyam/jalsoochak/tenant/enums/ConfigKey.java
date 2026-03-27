package org.arghyam.jalsoochak.tenant.enums;

/**
 * Sealed interface for configuration keys.
 * Restricts config keys to only TenantConfigKeyEnum and SystemConfigKeyEnum.
 * This provides type safety and clear domain modeling.
 */
public sealed interface ConfigKey permits TenantConfigKeyEnum, SystemConfigKeyEnum {
}
