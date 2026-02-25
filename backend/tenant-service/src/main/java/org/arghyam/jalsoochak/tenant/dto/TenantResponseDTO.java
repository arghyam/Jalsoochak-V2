package org.arghyam.jalsoochak.tenant.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Tenant details returned after creation or listing")
public class TenantResponseDTO {

    @Schema(description = "Auto-generated tenant ID", example = "1")
    private Integer id;

    @Schema(description = "UUID of the tenant", example = "a3f1b2c4-5678-90ab-cdef-1234567890ab")
    private String uuid;

    @Schema(description = "Unique state code", example = "KA")
    private String stateCode;

    @Schema(description = "Local Government Directory code", example = "29")
    private Integer lgdCode;

    @Schema(description = "Display name of the tenant", example = "Karnataka")
    private String name;

    @Schema(description = "Current status of the tenant", example = "ACTIVE", allowableValues = { "ACTIVE",
            "INACTIVE" })
    private String status;

    @Schema(description = "Timestamp when the tenant was created")
    private LocalDateTime createdAt;

    @Schema(description = "ID of the user who created this tenant", example = "1")
    private Integer createdBy;

    @Schema(description = "Timestamp when the tenant was onboarded")
    private LocalDateTime onboardedAt;

    @Schema(description = "Timestamp of the last update")
    private LocalDateTime updatedAt;

    @Schema(description = "ID of the user who last updated this tenant")
    private Integer updatedBy;

}
