package org.arghyam.jalsoochak.user.enums;

public enum TenantUserStatus {
    INACTIVE(0),
    ACTIVE(1);

    public final int code;

    TenantUserStatus(int code) {
        this.code = code;
    }

    public static TenantUserStatus fromCode(int code) {
        for (TenantUserStatus s : values()) {
            if (s.code == code) return s;
        }
        throw new IllegalArgumentException("Unknown tenant user status code: " + code);
    }
}
