package org.arghyam.jalsoochak.tenant.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.arghyam.jalsoochak.tenant.enums.TenantConfigKeyEnum;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response DTO containing all configurations for a tenant in a Map format")
public class TenantConfigResponseDTO {

    @Schema(description = "ID of the tenant", example = "1")
    private Integer tenantId;

    @Schema(description = "Map of configuration keys to their stringified values")
    private Map<TenantConfigKeyEnum, String> configs;
}
