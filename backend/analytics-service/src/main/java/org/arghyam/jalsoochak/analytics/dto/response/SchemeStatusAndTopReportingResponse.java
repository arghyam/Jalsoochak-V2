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
public class SchemeStatusAndTopReportingResponse {

    private Integer parentLgdId;
    private Integer parentDepartmentId;
    private String parentLgdCName;
    private String parentDepartmentCName;
    private String parentLgdTitle;
    private String parentDepartmentTitle;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer daysInRange;
    private Integer activeSchemeCount;
    private Integer inactiveSchemeCount;
    private Integer topSchemeCount;
    private List<TopReportingScheme> topSchemes;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopReportingScheme {
        private Integer schemeId;
        private String schemeName;
        private Integer statusCode;
        private String status;
        private Integer submissionDays;
        private BigDecimal reportingRate;
        private Integer immediateParentLgdId;
        private String immediateParentLgdCName;
        private String immediateParentLgdTitle;
        private Integer immediateParentDepartmentId;
        private String immediateParentDepartmentCName;
        private String immediateParentDepartmentTitle;
    }
}
