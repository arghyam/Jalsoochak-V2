package org.arghyam.jalsoochak.anomaly.dto.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnomalyEvent {

    private String eventType;
    private String uuid;
    private Integer tenantId;
    private Integer type;
    private Integer userId;
    private Integer schemeId;
    private BigDecimal aiReading;
    private BigDecimal aiConfidencePercentage;
    private BigDecimal overriddenReading;
    private Integer retries;
    private BigDecimal previousReading;
    private LocalDate previousReadingDate;
    private Integer consecutiveDaysMissed;
    private String reason;
    private Integer status;
    private String correlationId;
}
