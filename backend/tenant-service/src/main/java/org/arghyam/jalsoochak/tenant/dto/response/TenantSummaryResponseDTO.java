package org.arghyam.jalsoochak.tenant.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Aggregate status summary of all non-system tenants")
public class TenantSummaryResponseDTO {

    @Schema(description = "Total number of non-system tenants", example = "12")
    private long totalTenants;

    @Schema(description = "Number of tenants with ONBOARDED status", example = "2")
    private long onboardedTenants;

    @Schema(description = "Number of tenants with CONFIGURED status", example = "1")
    private long configuredTenants;

    @Schema(description = "Number of tenants with ACTIVE status", example = "7")
    private long activeTenants;

    @Schema(description = "Number of tenants with INACTIVE status", example = "1")
    private long inactiveTenants;

    @Schema(description = "Number of tenants with SUSPENDED status", example = "0")
    private long suspendedTenants;

    @Schema(description = "Number of tenants with DEGRADED status", example = "0")
    private long degradedTenants;

    @Schema(description = "Number of tenants with ARCHIVED status", example = "1")
    private long archivedTenants;
}
