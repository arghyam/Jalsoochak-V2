package org.arghyam.jalsoochak.user.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.util.List;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record SchemePumpOperatorsDTO(
        Long schemeId,
        String schemeName,
        List<PumpOperatorSummaryDTO> pumpOperators,
        Integer page,
        Integer size,
        Long totalPumpOperators,
        Integer totalPages
) {
}
