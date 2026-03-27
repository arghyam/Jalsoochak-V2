package org.arghyam.jalsoochak.analytics.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSubmissionStatusResponse {
    private Integer userId;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer schemeCount;
    private Integer compliantSubmissionCount;
    private Integer anomalousSubmissionCount;
    private List<DailySubmissionSchemeDistribution> dailySubmissionSchemeDistribution;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailySubmissionSchemeDistribution {
        private LocalDate date;
        private Integer submittedSchemeCount;
    }
}
