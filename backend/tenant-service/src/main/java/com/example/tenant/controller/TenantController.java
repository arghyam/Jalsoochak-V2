package com.example.tenant.controller;

import com.example.tenant.dto.CreateDepartmentRequestDTO;
import com.example.tenant.dto.CreateTenantRequestDTO;
import com.example.tenant.dto.DepartmentResponseDTO;
import com.example.tenant.dto.TenantResponseDTO;
import com.example.tenant.service.TenantManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;
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
@Tag(name = "Tenant Service", description = "APIs for tenant onboarding, schema provisioning, and tenant management")
public class TenantController {

    private final TenantManagementService tenantManagementService;

    @Operation(
            summary = "Create a new tenant",
            description = "Registers a new tenant in the common schema and provisions a dedicated "
                    + "database schema (tenant_<stateCode>) with all required tables and indexes.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Tenant created and schema provisioned successfully",
                    content = @Content(schema = @Schema(implementation = TenantResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request — missing or malformed fields"),
            @ApiResponse(responseCode = "409", description = "Tenant with the given state code already exists"),
            @ApiResponse(responseCode = "500", description = "Internal server error during tenant creation")
    })
    @PostMapping
    public ResponseEntity<TenantResponseDTO> createTenant(@Valid @RequestBody CreateTenantRequestDTO request) {
        log.info("POST /api/v1/tenants – Creating tenant: {}", request.getName());
        TenantResponseDTO response = tenantManagementService.createTenant(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
            summary = "List all tenants",
            description = "Returns every tenant registered in the common schema, ordered by ID.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of all tenants",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = TenantResponseDTO.class)))),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping
    public ResponseEntity<List<TenantResponseDTO>> getAllTenants() {
        log.info("GET /api/v1/tenants");
        return ResponseEntity.ok(tenantManagementService.getAllTenants());
    }

    @Operation(
            summary = "Get departments for the current tenant",
            description = "Fetches the department hierarchy from the tenant-specific schema. "
                    + "Requires the X-Tenant-Code header to be set by the API gateway.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of departments for the resolved tenant",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = DepartmentResponseDTO.class)))),
            @ApiResponse(responseCode = "400", description = "Tenant could not be resolved — missing X-Tenant-Code header"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/departments")
    public ResponseEntity<List<DepartmentResponseDTO>> getTenantDepartments() {
        log.info("GET /api/v1/tenants/departments");
        return ResponseEntity.ok(tenantManagementService.getTenantDepartments());
    }

    @Operation(
            summary = "Create a department for the current tenant",
            description = "Inserts a new department into the tenant-specific schema's department_master_table. "
                    + "Requires the X-Tenant-Code header to identify the target tenant schema.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Department created successfully",
                    content = @Content(schema = @Schema(implementation = DepartmentResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request or tenant could not be resolved"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/departments")
    public ResponseEntity<DepartmentResponseDTO> createDepartment(
            @Valid @RequestBody CreateDepartmentRequestDTO request) {
        log.info("POST /api/v1/tenants/departments – Creating department: {}", request.getTitle());
        DepartmentResponseDTO response = tenantManagementService.createDepartment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
