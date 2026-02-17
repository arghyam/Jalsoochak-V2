package com.example.tenant.config;

/**
 * Thread-local holder for the current tenant's schema name.
 * <p>
 * Populated by {@link TenantInterceptor} from the {@code X-Tenant-State} HTTP header,
 * or set programmatically before executing tenant-scoped queries.
 * The schema name follows the convention {@code tenant_<state_code_lowercase>}.
 */
public class TenantContext {

    private static final ThreadLocal<String> CURRENT_SCHEMA = new ThreadLocal<>();

    public static void setSchema(String schemaName) {
        CURRENT_SCHEMA.set(schemaName);
    }

    public static String getSchema() {
        return CURRENT_SCHEMA.get();
    }

    public static void clear() {
        CURRENT_SCHEMA.remove();
    }
}
