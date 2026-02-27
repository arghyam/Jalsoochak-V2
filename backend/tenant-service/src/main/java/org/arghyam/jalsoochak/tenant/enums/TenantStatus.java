package org.arghyam.jalsoochak.tenant.enums;

import java.util.Arrays;

public enum TenantStatus {

    ACTIVE(1),
    INACTIVE(0);

    private final int code;

    TenantStatus(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static TenantStatus fromCode(int code) {
        return Arrays.stream(values())
                .filter(s -> s.code == code)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown TenantStatus code: " + code));
    }
}
