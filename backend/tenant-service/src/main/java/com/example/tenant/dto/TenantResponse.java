package com.example.tenant.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TenantResponse {

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
}
