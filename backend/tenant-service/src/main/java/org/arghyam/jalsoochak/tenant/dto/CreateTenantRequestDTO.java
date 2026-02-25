package org.arghyam.jalsoochak.tenant.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request payload for creating a new tenant")
public class CreateTenantRequestDTO {

    @NotBlank(message = "State code is required")
    @Size(min = 2, max = 10, message = "State code must be 2-10 characters")
    @Pattern(regexp = "^[A-Za-z]+$", message = "State code must contain only alphabetic characters")
    @Schema(description = "Unique state code (2-10 alphabetic chars)", example = "KA", requiredMode = Schema.RequiredMode.REQUIRED)
    private String stateCode;

    @NotNull(message = "LGD code is required")
    @Schema(description = "Local Government Directory code for the state", example = "29", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer lgdCode;

    @NotBlank(message = "Tenant name is required")
    @Schema(description = "Display name of the tenant / state", example = "Karnataka", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

}
