package com.example.analytics.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "dim_department_location_table", schema = "analytics_schema")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DimDepartmentLocation {

    @Id
    @Column(name = "department_id")
    private Integer departmentId;

    @Column(name = "tenant_id", nullable = false)
    private Integer tenantId;

    @Column(name = "department_c_name")
    private String departmentCName;

    private String title;

    @Column(name = "department_level", nullable = false)
    private Integer departmentLevel;

    @Column(name = "level_1_dept_id", nullable = false)
    private Integer level1DeptId;

    @Column(name = "level_2_dept_id", nullable = false)
    private Integer level2DeptId;

    @Column(name = "level_3_dept_id", nullable = false)
    private Integer level3DeptId;

    @Column(name = "level_4_dept_id", nullable = false)
    private Integer level4DeptId;

    @Column(name = "level_5_dept_id", nullable = false)
    private Integer level5DeptId;

    @Column(name = "level_6_dept_id", nullable = false)
    private Integer level6DeptId;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
