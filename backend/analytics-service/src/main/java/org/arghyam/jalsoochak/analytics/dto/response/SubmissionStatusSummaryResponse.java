package org.arghyam.jalsoochak.analytics.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubmissionStatusSummaryResponse {
    private Integer schemeCount;
    private Integer compliantSubmissionCount;
    private Integer anomalousSubmissionCount;
}
