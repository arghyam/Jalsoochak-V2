package org.arghyam.jalsoochak.scheme.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SchemeUploadResponseDTO {
    private String message;
    private Integer totalRows;
    private Integer uploadedRows;
}
