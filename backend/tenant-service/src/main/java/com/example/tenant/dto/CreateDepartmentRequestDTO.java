package com.example.tenant.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Request payload for creating a department within a tenant schema")
public class CreateDepartmentRequestDTO {

    @NotBlank(message = "Department title is required")
    @Schema(description = "Department name", example = "Water Resources Zone A", requiredMode = Schema.RequiredMode.REQUIRED)
    private String title;

    @Schema(description = "FK to location_config_master_table defining the hierarchy level", example = "1")
    private Integer departmentLocationConfigId;

    @Schema(description = "Parent department ID (null for root departments)", example = "1")
    private Integer parentId;

    @NotNull(message = "Status is required")
    @Schema(description = "Status: 1 = ACTIVE, 0 = INACTIVE", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer status;

    @Schema(description = "ID of the admin user creating this department", example = "1")
    private Integer createdBy;

    public CreateDepartmentRequestDTO() {
    }

    public CreateDepartmentRequestDTO(String title, Integer departmentLocationConfigId,
                                       Integer parentId, Integer status, Integer createdBy) {
        this.title = title;
        this.departmentLocationConfigId = departmentLocationConfigId;
        this.parentId = parentId;
        this.status = status;
        this.createdBy = createdBy;
    }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public Integer getDepartmentLocationConfigId() { return departmentLocationConfigId; }
    public void setDepartmentLocationConfigId(Integer departmentLocationConfigId) { this.departmentLocationConfigId = departmentLocationConfigId; }

    public Integer getParentId() { return parentId; }
    public void setParentId(Integer parentId) { this.parentId = parentId; }

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }

    public Integer getCreatedBy() { return createdBy; }
    public void setCreatedBy(Integer createdBy) { this.createdBy = createdBy; }
}
