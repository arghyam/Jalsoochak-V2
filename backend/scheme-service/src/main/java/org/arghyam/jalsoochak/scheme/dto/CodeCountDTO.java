package org.arghyam.jalsoochak.scheme.dto;

import lombok.Builder;

@Builder
public record CodeCountDTO(
        int code,
        long count
) {
}

