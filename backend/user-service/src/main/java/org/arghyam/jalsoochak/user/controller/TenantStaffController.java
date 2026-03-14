package org.arghyam.jalsoochak.user.controller;

import lombok.RequiredArgsConstructor;
import org.arghyam.jalsoochak.user.dto.common.ApiResponseDTO;
import org.arghyam.jalsoochak.user.dto.common.PageResponseDTO;
import org.arghyam.jalsoochak.user.dto.response.RoleCountDTO;
import org.arghyam.jalsoochak.user.dto.response.TenantStaffResponseDTO;
import org.arghyam.jalsoochak.user.service.TenantStaffService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tenant")
@RequiredArgsConstructor
public class TenantStaffController {

    private final TenantStaffService tenantStaffService;

    @GetMapping("/staff")
    public ResponseEntity<ApiResponseDTO<PageResponseDTO<TenantStaffResponseDTO>>> listStaff(
            @RequestParam String tenantCode,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String name
    ) {
        return ResponseEntity.ok(ApiResponseDTO.of(200, "Staff retrieved",
                tenantStaffService.listStaff(tenantCode, page, limit, sortBy, sortDir, role, status, name)));
    }

    @GetMapping("/staff/counts/by-role")
    public ResponseEntity<ApiResponseDTO<List<RoleCountDTO>>> countStaffByRole(
            @RequestParam String tenantCode,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String name
    ) {
        return ResponseEntity.ok(ApiResponseDTO.of(200, "Staff counts retrieved",
                tenantStaffService.countStaffByRole(tenantCode, status, name)));
    }
}
