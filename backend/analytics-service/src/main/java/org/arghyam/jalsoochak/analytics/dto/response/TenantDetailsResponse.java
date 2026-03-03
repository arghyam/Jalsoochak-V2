package org.arghyam.jalsoochak.analytics.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenantDetailsResponse {

    private Integer tenantId;
    private String stateCode;
    private String schemaName;
    private Integer parentLgdId;
    private Integer childBoundaryCount;
    private String boundaryGeoJson;
    private List<ChildRegionDetails> childRegions;
}
