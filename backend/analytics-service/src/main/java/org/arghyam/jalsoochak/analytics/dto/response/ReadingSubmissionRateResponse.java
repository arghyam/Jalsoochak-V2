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
public class ReadingSubmissionRateResponse {

    private Integer parentLgdId;
    private Integer parentDepartmentId;
    private Integer parentLgdLevel;
    private Integer parentDepartmentLevel;
    private String scope;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer daysInRange;
    private Integer schemeCount;
    private Integer totalSubmissionDays;
    private BigDecimal readingSubmissionRate;
    private Integer childRegionCount;
    private List<ChildRegionReadingSubmissionRate> childRegions;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChildRegionReadingSubmissionRate {
        private Integer lgdId;
        private Integer departmentId;
        private String title;
        private Integer schemeCount;
        private Integer totalSubmissionDays;
        private BigDecimal readingSubmissionRate;
    }
}
