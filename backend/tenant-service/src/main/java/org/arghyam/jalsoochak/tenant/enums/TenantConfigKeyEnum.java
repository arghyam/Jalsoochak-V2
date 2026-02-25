package org.arghyam.jalsoochak.tenant.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Exhaustive list of allowed configuration keys for tenants.
 */
@Getter
@RequiredArgsConstructor
public enum TenantConfigKeyEnum {

    /**
     * JSON definition for email templates.
     */
    EMAIL_TEMPLATE_JSON,

    /**
     * URL for the tenant's logo.
     */
    TENANT_LOGO_URL
}
