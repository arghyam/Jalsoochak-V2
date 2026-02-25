package org.arghyam.jalsoochak.tenant.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
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
@Schema(description = "Request DTO to set or update multiple tenant configurations")
public class SetTenantConfigRequestDTO {

    @NotEmpty(message = "Configurations map cannot be empty")
    @Schema(description = "Map of configuration keys to their stringified values")
    private Map<TenantConfigKeyEnum, String> configs;
}
