package org.arghyam.jalsoochak.scheme.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SchemeDTO {
    private Integer id;
    private String uuid;
    private String stateSchemeId;
    private String centreSchemeId;
    private String schemeName;
    private Integer fhtcCount;
    private Integer plannedFhtc;
    private Integer houseHoldCount;
    private Double latitude;
    private Double longitude;
    private Integer channel;
    private String workStatus;
    private String operatingStatus;
}
