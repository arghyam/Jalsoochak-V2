package org.arghyam.jalsoochak.analytics.dto.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class SchemePerformanceEvent {

    private String eventType;
    private Integer tenantId;
    private Integer schemeId;
    private BigDecimal performanceScore;
    private String lastWaterSupplyDate;
}
