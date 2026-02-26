package org.arghyam.jalsoochak.tenant.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocationLevelNameDTO {
    private Integer languageId;
    private String title;
}
