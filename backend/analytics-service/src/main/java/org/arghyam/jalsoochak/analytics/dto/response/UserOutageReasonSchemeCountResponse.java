package org.arghyam.jalsoochak.analytics.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserOutageReasonSchemeCountResponse {
    private Integer userId;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer schemeCount;
    private Map<String, Integer> outageReasonSchemeCount;
}
