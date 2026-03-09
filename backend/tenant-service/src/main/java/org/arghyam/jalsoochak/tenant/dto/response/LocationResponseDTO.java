package org.arghyam.jalsoochak.tenant.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for location (LGD or Department) records within a tenant schema.
 * Minimal fields for tree UI rendering and hierarchy navigation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Location details (LGD or Department) within a tenant schema")
public class LocationResponseDTO {

    @Schema(description = "Auto-generated location ID", example = "1")
    private Integer id;

    @Schema(description = "UUID of the location")
    private String uuid;

    @Schema(description = "Location name / title", example = "Madhya Pradesh")
    private String title;

    @Schema(description = "LGD code (only for LGD hierarchy)", example = "MP001")
    private String lgdCode;

    @Schema(description = "ID of the parent location (null for root level)", example = "1")
    private Integer parentId;

    @Schema(description = "Status: 1 = ACTIVE, 0 = INACTIVE", example = "1")
    private Integer status;
}
