package org.arghyam.jalsoochak.user.dto.response;

import lombok.Builder;

@Builder
public record PumpOperatorUploadResponseDTO(
        String message,
        int totalRows,
        int uploadedRows,
        int skippedRows
) {
}

