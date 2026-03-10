package org.arghyam.jalsoochak.analytics.enums;

import java.util.Arrays;

public enum OutageReason {
    DRAUGHT(1, "draught"),
    NO_ELECTRICITY(2, "no_electricity"),
    MOTOR_BURNT(3, "motor_burnt");

    private final int code;
    private final String key;

    OutageReason(int code, String key) {
        this.code = code;
        this.key = key;
    }

    public int getCode() {
        return code;
    }

    public String getKey() {
        return key;
    }

    public static String getKeyForCode(Integer code) {
        if (code == null) {
            return "unknown";
        }
        return Arrays.stream(values())
                .filter(reason -> reason.code == code)
                .map(OutageReason::getKey)
                .findFirst()
                .orElse("unknown_" + code);
    }
}
