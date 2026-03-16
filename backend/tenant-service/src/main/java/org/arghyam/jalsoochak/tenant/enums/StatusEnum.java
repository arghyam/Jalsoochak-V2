package org.arghyam.jalsoochak.tenant.enums;

import java.util.Arrays;

/**
 * Generic active/inactive record status used across entity tables
 * (e.g. language, location). For tenant-specific statuses (including ARCHIVED)
 * use {@link TenantStatusEnum}.
 */
public enum StatusEnum {

    ACTIVE(1),
    INACTIVE(0);

    private final int code;

    StatusEnum(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static StatusEnum fromCode(int code) {
        return Arrays.stream(values())
                .filter(s -> s.code == code)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown StatusEnum code: " + code));
    }
}
