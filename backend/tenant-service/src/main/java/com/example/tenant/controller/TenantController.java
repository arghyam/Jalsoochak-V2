package com.example.tenant.controller;

import com.example.tenant.dto.CreateTenantRequest;
import com.example.tenant.dto.DepartmentResponse;
import com.example.tenant.dto.TenantResponse;
import com.example.tenant.service.TenantManagementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tenants")
@RequiredArgsConstructor
@Slf4j
public class TenantController {

    private final TenantManagementService tenantManagementService;

    @PostMapping
    public ResponseEntity<TenantResponse> createTenant(@RequestBody CreateTenantRequest request) {
        log.info("POST /api/v1/tenants – Creating tenant: {}", request.getName());
        TenantResponse response = tenantManagementService.createTenant(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<TenantResponse>> getAllTenants() {
        log.info("GET /api/v1/tenants");
        return ResponseEntity.ok(tenantManagementService.getAllTenants());
    }

    @GetMapping("/departments")
    public ResponseEntity<List<DepartmentResponse>> getTenantDepartments() {
        log.info("GET /api/v1/tenants/departments");
        return ResponseEntity.ok(tenantManagementService.getTenantDepartments());
    }
}
