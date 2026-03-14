package org.arghyam.jalsoochak.user.util;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.regex.Pattern;

public final class TenantSchemaResolver {

    private static final Pattern SAFE_TENANT_CODE = Pattern.compile("^[A-Za-z0-9_]{1,32}$");

    private TenantSchemaResolver() {
    }

    public static String requireSchemaNameFromTenantCode(String tenantCode) {
        if (tenantCode == null || tenantCode.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "tenantCode is required");
        }
        String normalized = tenantCode.trim();
        if (!SAFE_TENANT_CODE.matcher(normalized).matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid tenantCode format");
        }
        return "tenant_" + normalized.toLowerCase();
    }
}

