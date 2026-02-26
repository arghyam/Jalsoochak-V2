package org.arghyam.jalsoochak.tenant.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Global configuration keys for system-level platform settings.
 * These are managed by Super Admins and serve as defaults.
 */
@Getter
@RequiredArgsConstructor
public enum SystemConfigKeyEnum {

    /**
     * Default LGD (Local Government Directory) location hierarchy configuration.
     * Stored in common_schema.tenant_config_master_table with tenant_id = 0.
     */
    DEFAULT_LGD_LOCATION_HIERARCHY,

    /**
     * Default department location hierarchy configuration.
     * Stored in common_schema.tenant_config_master_table with tenant_id = 0.
     */
    DEFAULT_DEPT_LOCATION_HIERARCHY;

}
