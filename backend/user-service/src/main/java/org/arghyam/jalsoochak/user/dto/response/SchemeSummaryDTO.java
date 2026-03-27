package org.arghyam.jalsoochak.user.dto.response;

import lombok.Builder;

@Builder
public record SchemeSummaryDTO(
        Long schemeId,
        String schemeName,
        String workStatus,
        String operatingStatus
) {
}
