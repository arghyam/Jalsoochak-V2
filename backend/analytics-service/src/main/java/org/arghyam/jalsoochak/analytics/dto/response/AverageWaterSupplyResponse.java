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
public class AverageWaterSupplyResponse {

    private Integer tenantId;
    private Integer lgdId;
    private Integer parentDepartmentId;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer daysInRange;
    private Integer schemeCount;
    private Integer childRegionCount;
    private List<SchemeWaterSupply> schemes;
    private List<ChildRegionWaterSupply> childRegions;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SchemeWaterSupply {
        private Integer schemeId;
        private String schemeName;
        private Integer householdCount;
        private Long totalWaterSuppliedLiters;
        private Integer supplyDays;
        private BigDecimal avgLitersPerHousehold;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChildRegionWaterSupply {
        private Integer tenantId;
        private String stateCode;
        private Integer lgdId;
        private Integer departmentId;
        private String title;
        private Integer totalHouseholdCount;
        private Long totalWaterSuppliedLiters;
        private Integer schemeCount;
        private BigDecimal avgWaterSupplyPerScheme;
    }
}
