package org.arghyam.jalsoochak.analytics.dto.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class DepartmentLocationEvent {

    private String eventType;
    private Integer departmentId;
    private Integer tenantId;
    private String departmentCName;
    private String title;
    private Integer departmentLevel;
    private Integer level1DeptId;
    private Integer level2DeptId;
    private Integer level3DeptId;
    private Integer level4DeptId;
    private Integer level5DeptId;
    private Integer level6DeptId;
}
