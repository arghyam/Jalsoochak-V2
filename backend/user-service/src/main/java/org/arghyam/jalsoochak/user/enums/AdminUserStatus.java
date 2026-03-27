package org.arghyam.jalsoochak.user.enums;

public enum AdminUserStatus {
    INACTIVE(0),
    ACTIVE(1),
    PENDING(2);

    public final int code;

    AdminUserStatus(int code) {
        this.code = code;
    }

    public static AdminUserStatus fromCode(int code) {
        for (AdminUserStatus s : values()) {
            if (s.code == code) return s;
        }
        throw new IllegalArgumentException("Unknown admin user status code: " + code);
    }
}
