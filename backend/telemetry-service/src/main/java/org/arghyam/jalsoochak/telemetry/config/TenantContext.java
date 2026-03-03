package org.arghyam.jalsoochak.telemetry.config;

public class TenantContext {
    private static final ThreadLocal<String> CURRENT_SCHEMA = new ThreadLocal<>();

    private TenantContext() {
    }

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
