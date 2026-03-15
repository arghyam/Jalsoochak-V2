package org.arghyam.jalsoochak.user.dto.response;

import lombok.Builder;

@Builder
public record PumpOperatorSummaryDTO(
        Long id,
        String uuid,
        String name,
        String email,
        String phoneNumber,
        Integer status
) {
}

