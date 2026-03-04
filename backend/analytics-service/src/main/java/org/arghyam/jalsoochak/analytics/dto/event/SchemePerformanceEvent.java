package org.arghyam.jalsoochak.analytics.dto.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class SchemePerformanceEvent {

    private String eventType;
    private Integer tenantId;
    private Integer schemeId;
    private Integer performanceScore;
    private String lastWaterSupplyDate;
}
