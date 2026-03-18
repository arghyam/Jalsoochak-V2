package org.arghyam.jalsoochak.analytics.enums;

import java.util.Arrays;

public enum SubmissionStatus {
    SUBMITTED(1),
    NOT_SUBMITTED(0);

    private final int code;

    SubmissionStatus(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static SubmissionStatus fromCode(int code) {
        return Arrays.stream(values())
                .filter(status -> status.code == code)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown SubmissionStatus code: " + code));
    }
}
