package org.arghyam.jalsoochak.scheme.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiErrorResponseDTO {
    private OffsetDateTime timestamp;
    private Integer status;
    private String error;
    private String message;
    private List<SchemeUploadErrorDTO> errors;
}
