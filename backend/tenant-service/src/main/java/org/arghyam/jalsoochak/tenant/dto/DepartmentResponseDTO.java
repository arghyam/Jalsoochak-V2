package org.arghyam.jalsoochak.tenant.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Department details within a tenant schema")
public class DepartmentResponseDTO {

    @Schema(description = "Auto-generated department ID", example = "1")
    private Integer id;

    @Schema(description = "UUID of the department")
    private String uuid;

    @Schema(description = "Department name", example = "Water Resources Zone A")
    private String title;

    @Schema(description = "FK to location_config_master_table", example = "1")
    private Integer departmentLocationConfigId;

    @Schema(description = "ID of the parent department (null for root)", example = "1")
    private Integer parentId;

    @Schema(description = "Status: 1 = ACTIVE, 0 = INACTIVE", example = "1")
    private Integer status;

    @Schema(description = "Timestamp when the department was created")
    private LocalDateTime createdAt;

    @Schema(description = "ID of the user who created this department")
    private Integer createdBy;

    @Schema(description = "Timestamp of the last update")
    private LocalDateTime updatedAt;

    @Schema(description = "ID of the user who last updated this department")
    private Integer updatedBy;

    public DepartmentResponseDTO() {
    }

    public DepartmentResponseDTO(Integer id, String uuid, String title, Integer departmentLocationConfigId,
                                  Integer parentId, Integer status, LocalDateTime createdAt, Integer createdBy,
                                  LocalDateTime updatedAt, Integer updatedBy) {
        this.id = id;
        this.uuid = uuid;
        this.title = title;
        this.departmentLocationConfigId = departmentLocationConfigId;
        this.parentId = parentId;
        this.status = status;
        this.createdAt = createdAt;
        this.createdBy = createdBy;
        this.updatedAt = updatedAt;
        this.updatedBy = updatedBy;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getUuid() { return uuid; }
    public void setUuid(String uuid) { this.uuid = uuid; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public Integer getDepartmentLocationConfigId() { return departmentLocationConfigId; }
    public void setDepartmentLocationConfigId(Integer departmentLocationConfigId) { this.departmentLocationConfigId = departmentLocationConfigId; }

    public Integer getParentId() { return parentId; }
    public void setParentId(Integer parentId) { this.parentId = parentId; }

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public Integer getCreatedBy() { return createdBy; }
    public void setCreatedBy(Integer createdBy) { this.createdBy = createdBy; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public Integer getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(Integer updatedBy) { this.updatedBy = updatedBy; }

    public static DepartmentResponseDTOBuilder builder() { return new DepartmentResponseDTOBuilder(); }

    public static class DepartmentResponseDTOBuilder {
        private Integer id;
        private String uuid;
        private String title;
        private Integer departmentLocationConfigId;
        private Integer parentId;
        private Integer status;
        private LocalDateTime createdAt;
        private Integer createdBy;
        private LocalDateTime updatedAt;
        private Integer updatedBy;

        public DepartmentResponseDTOBuilder id(Integer id) { this.id = id; return this; }
        public DepartmentResponseDTOBuilder uuid(String uuid) { this.uuid = uuid; return this; }
        public DepartmentResponseDTOBuilder title(String title) { this.title = title; return this; }
        public DepartmentResponseDTOBuilder departmentLocationConfigId(Integer departmentLocationConfigId) { this.departmentLocationConfigId = departmentLocationConfigId; return this; }
        public DepartmentResponseDTOBuilder parentId(Integer parentId) { this.parentId = parentId; return this; }
        public DepartmentResponseDTOBuilder status(Integer status) { this.status = status; return this; }
        public DepartmentResponseDTOBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public DepartmentResponseDTOBuilder createdBy(Integer createdBy) { this.createdBy = createdBy; return this; }
        public DepartmentResponseDTOBuilder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }
        public DepartmentResponseDTOBuilder updatedBy(Integer updatedBy) { this.updatedBy = updatedBy; return this; }

        public DepartmentResponseDTO build() {
            return new DepartmentResponseDTO(id, uuid, title, departmentLocationConfigId,
                    parentId, status, createdAt, createdBy, updatedAt, updatedBy);
        }
    }
}
