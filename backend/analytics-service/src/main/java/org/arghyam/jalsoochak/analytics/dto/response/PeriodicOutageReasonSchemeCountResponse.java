package org.arghyam.jalsoochak.analytics.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PeriodicOutageReasonSchemeCountResponse {

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
        private Map<String, Integer> outageReasonSchemeCount;
    }
}
