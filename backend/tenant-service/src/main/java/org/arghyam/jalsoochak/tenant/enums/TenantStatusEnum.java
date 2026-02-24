package org.arghyam.jalsoochak.tenant.enums;

import java.util.Arrays;

public enum TenantStatusEnum {

    ACTIVE(1),
    INACTIVE(0),
    ARCHIVED(2);

    private final int code;

    TenantStatusEnum(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static TenantStatusEnum fromCode(int code) {
        return Arrays.stream(values())
                .filter(s -> s.code == code)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown TenantStatusEnum code: " + code));
    }
}
