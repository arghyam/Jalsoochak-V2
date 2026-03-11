package org.arghyam.jalsoochak.user.dto.response;

import lombok.Builder;

@Builder
public record UploadErrorDTO(
        int row,
        String field,
        String message
) {
}

