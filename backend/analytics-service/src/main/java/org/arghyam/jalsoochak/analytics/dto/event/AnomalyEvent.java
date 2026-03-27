package org.arghyam.jalsoochak.analytics.dto.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
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
