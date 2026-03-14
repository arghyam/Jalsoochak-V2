package org.arghyam.jalsoochak.user.dto.response;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
public record PumpOperatorReadingComplianceRowDTO(
        Long id,
        String uuid,
        String name,
        LocalDateTime lastSubmissionAt,
        BigDecimal confirmedReading
) {
}

