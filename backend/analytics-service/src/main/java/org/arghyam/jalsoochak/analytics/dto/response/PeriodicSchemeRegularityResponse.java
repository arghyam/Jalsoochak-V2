package org.arghyam.jalsoochak.analytics.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PeriodicSchemeRegularityResponse {

    private Integer lgdId;
    private Integer departmentId;
    private String scale;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer periodCount;
    private List<PeriodicMetric> metrics;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PeriodicMetric {
        private LocalDate periodStartDate;
        private LocalDate periodEndDate;
        private Integer schemeCount;
        private Integer totalSupplyDays;
        private BigDecimal averageRegularity;
    }
}

