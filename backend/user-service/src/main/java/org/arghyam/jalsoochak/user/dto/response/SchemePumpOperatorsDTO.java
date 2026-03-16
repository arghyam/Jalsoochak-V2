package org.arghyam.jalsoochak.user.dto.response;

import lombok.Builder;

import java.util.List;

@Builder
public record SchemePumpOperatorsDTO(
        Long schemeId,
        String schemeName,
        List<PumpOperatorSummaryDTO> pumpOperators
) {
}

