package org.arghyam.jalsoochak.scheme.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SchemeUploadErrorDTO {
    private Integer rowNumber;
    private String field;
    private String message;
}
