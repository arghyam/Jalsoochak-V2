package org.arghyam.jalsoochak.tenant.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Request payload for updating an existing tenant")
public class UpdateTenantRequestDTO {

    @Schema(description = "Updated status of the tenant", example = "ACTIVE", allowableValues = { "ACTIVE",
            "ARCHIVED" })
    private String status;

    public UpdateTenantRequestDTO() {
    }

    public UpdateTenantRequestDTO(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
