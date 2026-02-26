package com.example.telemetry.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class TenantContextTest {

    @Test
    void setGetAndClearSchemaWorks() {
        TenantContext.clear();
        assertNull(TenantContext.getSchema());

        TenantContext.setSchema("tenant_mp");
        assertEquals("tenant_mp", TenantContext.getSchema());

        TenantContext.clear();
        assertNull(TenantContext.getSchema());
    }
}
