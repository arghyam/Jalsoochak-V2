package org.arghyam.jalsoochak.tenant.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Exhaustive list of allowed configuration keys for tenants.
 * Each key has a Scope (who can edit) and a Type (how it is stored).
 */
@Getter
@RequiredArgsConstructor
public enum TenantConfigKeyEnum {

    /**
     * JSON definition for email templates.
     */
    EMAIL_TEMPLATE_JSON(ConfigType.GENERIC),

    /**
     * URL for the tenant's logo.
     */
    TENANT_LOGO_URL(ConfigType.GENERIC),

    /**
     * List of supported languages for the tenant.
     * Stored in tenant-specific language_master_table.
     */
    SUPPORTED_LANGUAGES(ConfigType.SPECIALIZED),

    /**
     * LGD (Local Government Directory) location hierarchy configuration.
     * Stored in tenant-specific location_config_master_table with region_type LGD.
     */
    LGD_LOCATION_HIERARCHY(ConfigType.SPECIALIZED),

    /**
     * Department location hierarchy configuration.
     * Stored in tenant-specific location_config_master_table with region_type
     * DEPARTMENT.
     */
    DEPT_LOCATION_HIERARCHY(ConfigType.SPECIALIZED);

    private final ConfigType type;

    public enum ConfigType {
        GENERIC, // Stored as KV in common_schema.tenant_config_master_table
        SPECIALIZED // Stored in specific tables in tenant schema
    }
}
