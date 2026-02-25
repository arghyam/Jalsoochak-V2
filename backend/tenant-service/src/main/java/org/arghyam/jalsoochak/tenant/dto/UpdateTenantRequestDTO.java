package org.arghyam.jalsoochak.tenant.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request payload for updating an existing tenant")
public class UpdateTenantRequestDTO {

    @Schema(description = "Updated status of the tenant", example = "ACTIVE", allowableValues = { "ACTIVE",
            "ARCHIVED" })
    private String status;

}
