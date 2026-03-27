package org.arghyam.jalsoochak.analytics.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
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
public class NonSubmissionReasonSchemeCountResponse {

    private Integer lgdId;
    private Integer departmentId;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer parentLgdLevel;
    private Integer parentDepartmentLevel;
    private Map<String, Integer> nonSubmissionReasonSchemeCount;
    private Integer childRegionCount;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<ChildRegionNonSubmissionReasonSchemeCount> childRegions;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChildRegionNonSubmissionReasonSchemeCount {
        private Integer lgdId;
        private Integer departmentId;
        private String title;
        private Map<String, Integer> nonSubmissionReasonSchemeCount;
    }
}
