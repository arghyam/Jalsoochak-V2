package org.arghyam.jalsoochak.scheme.dto;

import lombok.Builder;

@Builder
public record SchemeMappingDTO(
        Long id,
        Integer schemeId,
        String stateSchemeId,
        String schemeName,
        String villageLgdCode,
        String subDivisionName
) {
}

