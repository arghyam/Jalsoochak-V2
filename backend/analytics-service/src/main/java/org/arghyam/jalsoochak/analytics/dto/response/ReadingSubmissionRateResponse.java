package org.arghyam.jalsoochak.analytics.dto.response;

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
public class ReadingSubmissionRateResponse {

    private Integer parentLgdId;
    private Integer parentDepartmentId;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer daysInRange;
    private Integer schemeCount;
    private Integer totalSubmissionDays;
    private BigDecimal readingSubmissionRate;
}
