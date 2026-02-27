package org.arghyam.jalsoochak.tenant.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

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

    @Schema(description = "ID of the admin user creating this tenant", example = "1")
    private Integer createdBy;

    public CreateTenantRequestDTO() {
    }

    public CreateTenantRequestDTO(String stateCode, Integer lgdCode, String name, Integer createdBy) {
        this.stateCode = stateCode;
        this.lgdCode = lgdCode;
        this.name = name;
        this.createdBy = createdBy;
    }

    public String getStateCode() { return stateCode; }
    public void setStateCode(String stateCode) { this.stateCode = stateCode; }

    public Integer getLgdCode() { return lgdCode; }
    public void setLgdCode(Integer lgdCode) { this.lgdCode = lgdCode; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Integer getCreatedBy() { return createdBy; }
    public void setCreatedBy(Integer createdBy) { this.createdBy = createdBy; }
}
