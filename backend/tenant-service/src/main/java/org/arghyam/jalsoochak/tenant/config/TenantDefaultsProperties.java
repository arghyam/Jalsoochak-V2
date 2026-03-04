package org.arghyam.jalsoochak.tenant.config;

import java.util.List;

import org.arghyam.jalsoochak.tenant.dto.internal.LocationLevelConfigDTO;
import org.arghyam.jalsoochak.tenant.dto.internal.ReasonItemDTO;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@ConfigurationProperties(prefix = "tenant.defaults")
@Validated
@Data
public class TenantDefaultsProperties {

    @NotEmpty
    private List<LocationLevelConfigDTO> lgdLocationHierarchy;

    @NotEmpty
    private List<LocationLevelConfigDTO> deptLocationHierarchy;

    @NotEmpty
    private List<ReasonItemDTO> meterChangeReasons;
}
