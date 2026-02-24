package com.example.scheme.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SampleDTO {

    private Long id;
    private String schemeName;
    private String schemeCode;
    private Integer channel;
}
