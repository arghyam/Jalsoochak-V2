package org.arghyam.jalsoochak.tenant.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.arghyam.jalsoochak.tenant.enums.TenantConfigKeyEnum;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InternalTenantConfigDTO {
    private Integer id;
    private String uuid;
    private Integer tenantId;
    private TenantConfigKeyEnum configKey;
    private String configValue;
    private LocalDateTime createdAt;
    private Integer createdBy;
    private LocalDateTime updatedAt;
    private Integer updatedBy;
}
