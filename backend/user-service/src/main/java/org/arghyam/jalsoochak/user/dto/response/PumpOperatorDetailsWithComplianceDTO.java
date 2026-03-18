package org.arghyam.jalsoochak.user.dto.response;

import lombok.Builder;

@Builder
public record PumpOperatorDetailsWithComplianceDTO(
        PumpOperatorDetailsDTO details,
        PumpOperatorReadingComplianceDTO readingCompliance
) {
}
