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
public class UserNonSubmissionReasonSchemeCountResponse {
    private Integer userId;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer schemeCount;
    private Map<String, Integer> nonSubmissionReasonSchemeCount;
    private List<DailyNonSubmissionReasonDistribution> dailyNonSubmissionReasonDistribution;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyNonSubmissionReasonDistribution {
        private LocalDate date;
        private Map<String, Integer> nonSubmissionReasonSchemeCount;
    }
}
