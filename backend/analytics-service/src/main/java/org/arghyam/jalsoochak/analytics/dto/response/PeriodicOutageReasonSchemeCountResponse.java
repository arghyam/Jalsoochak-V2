package org.arghyam.jalsoochak.analytics.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Periodic outage-reason counts per calendar bucket. Nested type is named {@link PeriodicOutageMetric} (not
 * {@code PeriodicMetric}) so OpenAPI does not merge this schema with {@link PeriodicSchemeRegularityResponse}
 * or {@link PeriodicWaterQuantityResponse}, which also used a nested {@code PeriodicMetric} class name.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(
        name = "PeriodicOutageReasonSchemeCountResponse",
        description = "Outage reason → distinct scheme counts per time period for the selected LGD or department.")
public class PeriodicOutageReasonSchemeCountResponse {

    @Schema(nullable = true, description = "Present when the request used lgd_id.")
    private Integer lgdId;

    @Schema(nullable = true, description = "Present when the request used department_id.")
    private Integer departmentId;

    @Schema(example = "week", allowableValues = {"day", "week", "month"})
    private String scale;

    @Schema(example = "2026-01-01")
    private LocalDate startDate;

    @Schema(example = "2026-01-31")
    private LocalDate endDate;

    @Schema(example = "5")
    private Integer periodCount;

    private List<PeriodicOutageMetric> metrics;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(
            name = "PeriodicOutageMetric",
            description = "One calendar period: distinct schemes per outage reason (empty map when no outages in the period).")
    public static class PeriodicOutageMetric {

        @Schema(example = "2026-01-05")
        private LocalDate periodStartDate;

        @Schema(example = "2026-01-11")
        private LocalDate periodEndDate;

        @Schema(
                example = "{\"no_electricity\": 3, \"draught\": 1}",
                description = "Map of outage_reason to count of distinct schemes with that reason in this period.")
        private Map<String, Integer> outageReasonSchemeCount;
    }
}
