package org.arghyam.jalsoochak.tenant.enums;

import java.util.Arrays;
import lombok.Getter;

@Getter
public enum RegionTypeEnum {
    LGD(1),
    DEPARTMENT(2);

    private final int code;

    RegionTypeEnum(int code) {
        this.code = code;
    }

    public static RegionTypeEnum fromCode(int code) {
        return Arrays.stream(values())
                .filter(s -> s.code == code)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown RegionTypeEnum code: " + code));
    }
}
