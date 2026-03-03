package org.arghyam.jalsoochak.tenant.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

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

    @Schema(description = "Current status of the tenant", example = "ACTIVE", allowableValues = {"ACTIVE", "INACTIVE"})
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

    public TenantResponseDTO() {
    }

    public TenantResponseDTO(Integer id, String uuid, String stateCode, Integer lgdCode, String name,
                           String status, LocalDateTime createdAt, Integer createdBy,
                           LocalDateTime onboardedAt, LocalDateTime updatedAt, Integer updatedBy) {
        this.id = id;
        this.uuid = uuid;
        this.stateCode = stateCode;
        this.lgdCode = lgdCode;
        this.name = name;
        this.status = status;
        this.createdAt = createdAt;
        this.createdBy = createdBy;
        this.onboardedAt = onboardedAt;
        this.updatedAt = updatedAt;
        this.updatedBy = updatedBy;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getUuid() { return uuid; }
    public void setUuid(String uuid) { this.uuid = uuid; }

    public String getStateCode() { return stateCode; }
    public void setStateCode(String stateCode) { this.stateCode = stateCode; }

    public Integer getLgdCode() { return lgdCode; }
    public void setLgdCode(Integer lgdCode) { this.lgdCode = lgdCode; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public Integer getCreatedBy() { return createdBy; }
    public void setCreatedBy(Integer createdBy) { this.createdBy = createdBy; }

    public LocalDateTime getOnboardedAt() { return onboardedAt; }
    public void setOnboardedAt(LocalDateTime onboardedAt) { this.onboardedAt = onboardedAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public Integer getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(Integer updatedBy) { this.updatedBy = updatedBy; }

    public static TenantResponseDTOBuilder builder() { return new TenantResponseDTOBuilder(); }

    public static class TenantResponseDTOBuilder {
        private Integer id;
        private String uuid;
        private String stateCode;
        private Integer lgdCode;
        private String name;
        private String status;
        private LocalDateTime createdAt;
        private Integer createdBy;
        private LocalDateTime onboardedAt;
        private LocalDateTime updatedAt;
        private Integer updatedBy;

        public TenantResponseDTOBuilder id(Integer id) { this.id = id; return this; }
        public TenantResponseDTOBuilder uuid(String uuid) { this.uuid = uuid; return this; }
        public TenantResponseDTOBuilder stateCode(String stateCode) { this.stateCode = stateCode; return this; }
        public TenantResponseDTOBuilder lgdCode(Integer lgdCode) { this.lgdCode = lgdCode; return this; }
        public TenantResponseDTOBuilder name(String name) { this.name = name; return this; }
        public TenantResponseDTOBuilder status(String status) { this.status = status; return this; }
        public TenantResponseDTOBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public TenantResponseDTOBuilder createdBy(Integer createdBy) { this.createdBy = createdBy; return this; }
        public TenantResponseDTOBuilder onboardedAt(LocalDateTime onboardedAt) { this.onboardedAt = onboardedAt; return this; }
        public TenantResponseDTOBuilder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }
        public TenantResponseDTOBuilder updatedBy(Integer updatedBy) { this.updatedBy = updatedBy; return this; }

        public TenantResponseDTO build() {
            return new TenantResponseDTO(id, uuid, stateCode, lgdCode, name, status,
                    createdAt, createdBy, onboardedAt, updatedAt, updatedBy);
        }
    }
}
