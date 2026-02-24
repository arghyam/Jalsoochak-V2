package org.arghyam.jalsoochak.tenant.controller;

import org.arghyam.jalsoochak.tenant.dto.ApiResponseDTO;
import org.arghyam.jalsoochak.tenant.dto.CreateDepartmentRequestDTO;
import org.arghyam.jalsoochak.tenant.dto.CreateTenantRequestDTO;
import org.arghyam.jalsoochak.tenant.dto.DepartmentResponseDTO;
import org.arghyam.jalsoochak.tenant.dto.TenantResponseDTO;
import org.arghyam.jalsoochak.tenant.dto.UpdateTenantRequestDTO;
import org.arghyam.jalsoochak.tenant.service.TenantManagementService;
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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tenants")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Tenant Management", description = "APIs for tenant onboarding, schema provisioning, and tenant management")
public class TenantController {

        private final TenantManagementService tenantManagementService;

        /**
         * 1. API for creating a new tenant - accessible only by super admin
         */
        @Operation(summary = "Create a new tenant", description = "Registers a new tenant in the common schema and provisions a dedicated "
                        + "database schema (tenant_<stateCode>) with all required tables and indexes.")
        @ApiResponses({
                        @ApiResponse(responseCode = "201", description = "Tenant created and schema provisioned successfully", content = @Content(schema = @Schema(implementation = TenantResponseDTO.class))),
                        @ApiResponse(responseCode = "400", description = "Invalid request — missing or malformed fields"),
                        @ApiResponse(responseCode = "409", description = "Tenant with the given state code already exists"),
                        @ApiResponse(responseCode = "500", description = "Internal server error during tenant creation")
        })
        // TODO: Change this to role based authorization for SUPER_ADMIN
        @PreAuthorize("permitAll")
        @PostMapping
        public ResponseEntity<ApiResponseDTO<TenantResponseDTO>> createTenant(
                        @Valid @RequestBody CreateTenantRequestDTO request) {
                log.info("POST /api/v1/tenants – Creating tenant: {}", request.getName());
                TenantResponseDTO tenant = tenantManagementService.createTenant(request);
                return ResponseEntity.status(HttpStatus.CREATED)
                                .body(ApiResponseDTO.of(201, "Tenant created successfully", tenant));
        }

        /**
         * 2. API for getting all tenants - accessible by super admin and tenant admin
         */
        @Operation(summary = "List all tenants", description = "Returns every tenant registered in the common schema, ordered by ID.")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "List of all tenants", content = @Content(array = @ArraySchema(schema = @Schema(implementation = TenantResponseDTO.class)))),
                        @ApiResponse(responseCode = "500", description = "Internal server error")
        })
        // TODO: Change this to role based authorization for SUPER_ADMIN and TENANT_ADMIN
        @PreAuthorize("permitAll")
        @GetMapping
        public ResponseEntity<ApiResponseDTO<List<TenantResponseDTO>>> getAllTenants() {
                log.info("GET /api/v1/tenants");
                return ResponseEntity.ok(ApiResponseDTO.of(200, "Tenants retrieved successfully",
                                tenantManagementService.getAllTenants()));
        }

        /**
         * 3. API for updating a tenant - accessible by super admin
         */
        @Operation(summary = "Update tenant", description = "Updates the status of an existing tenant identified by tenantId.")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Tenant updated successfully", content = @Content(schema = @Schema(implementation = TenantResponseDTO.class))),
                        @ApiResponse(responseCode = "400", description = "Tenant updation failed"),
                        @ApiResponse(responseCode = "404", description = "Tenant with given tenantId does not exist"),
                        @ApiResponse(responseCode = "500", description = "Internal server error")
        })
        // TODO: Change this to role based authorization for SUPER_ADMIN
        @PreAuthorize("permitAll")
        @PutMapping("/{tenantId}")
        public ResponseEntity<ApiResponseDTO<TenantResponseDTO>> updateTenant(
                        @PathVariable Integer tenantId,
                        @Valid @RequestBody UpdateTenantRequestDTO request) {
                log.info("PUT /api/v1/tenants/{}", tenantId);
                TenantResponseDTO updated = tenantManagementService.updateTenant(tenantId, request);
                return ResponseEntity.ok(ApiResponseDTO.of(200, "Tenant updated successfully", updated));
        }

        /**
         * 4. API for deactivating a tenant - accessible by super admin
         */
        @Operation(summary = "Deactivate (soft-delete) a tenant", description = "Sets the tenant status to INACTIVE for the given tenantId.")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Tenant deactivated successfully"),
                        @ApiResponse(responseCode = "400", description = "Tenant deactivation failed"),
                        @ApiResponse(responseCode = "404", description = "Tenant with given tenantId does not exist"),
                        @ApiResponse(responseCode = "500", description = "Internal server error")
        })
        // TODO: Change this to role based authorization for SUPER_ADMIN
        @PreAuthorize("permitAll")
        @DeleteMapping("/{tenantId}")
        public ResponseEntity<ApiResponseDTO<Void>> deactivateTenant(@PathVariable Integer tenantId) {
                log.info("DELETE /api/v1/tenants/{}", tenantId);
                tenantManagementService.deactivateTenant(tenantId);
                return ResponseEntity.ok(ApiResponseDTO.of(200, "Tenant deactivated successfully"));
        }

        /**
         * 5. API for getting departments for the current tenant - accessible by super
         * admin and tenant admin
         */
        @Operation(summary = "Get departments for the current tenant", description = "Fetches the department hierarchy from the tenant-specific schema. "
                        + "Requires the X-Tenant-Code header to be set by the API gateway.")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "List of departments for the resolved tenant", content = @Content(array = @ArraySchema(schema = @Schema(implementation = DepartmentResponseDTO.class)))),
                        @ApiResponse(responseCode = "400", description = "Tenant could not be resolved — missing X-Tenant-Code header"),
                        @ApiResponse(responseCode = "500", description = "Internal server error")
        })
        // TODO: Change this to role based authorization for SUPER_ADMIN and TENANT_ADMIN
        @PreAuthorize("permitAll")
        @GetMapping("/departments")
        public ResponseEntity<ApiResponseDTO<List<DepartmentResponseDTO>>> getTenantDepartments() {
                log.info("GET /api/v1/tenants/departments");
                return ResponseEntity.ok(ApiResponseDTO.of(200, "Departments retrieved successfully",
                                tenantManagementService.getTenantDepartments()));
        }

        /**
         * 6. API for creating a department for the current tenant - accessible by super
         * admin and tenant admin
         */
        @Operation(summary = "Create a department for the current tenant", description = "Inserts a new department into the tenant-specific schema's department_location_master_table. "
                        + "Requires the X-Tenant-Code header to identify the target tenant schema.")
        @ApiResponses({
                        @ApiResponse(responseCode = "201", description = "Department created successfully", content = @Content(schema = @Schema(implementation = DepartmentResponseDTO.class))),
                        @ApiResponse(responseCode = "400", description = "Invalid request or tenant could not be resolved"),
                        @ApiResponse(responseCode = "500", description = "Internal server error")
        })
        // TODO: Change this to role based authorization for SUPER_ADMIN and TENANT_ADMIN
        @PreAuthorize("permitAll")
        @PostMapping("/departments")
        public ResponseEntity<ApiResponseDTO<DepartmentResponseDTO>> createDepartment(
                        @Valid @RequestBody CreateDepartmentRequestDTO request) {
                log.info("POST /api/v1/tenants/departments – Creating department: {}", request.getTitle());
                DepartmentResponseDTO dept = tenantManagementService.createDepartment(request);
                return ResponseEntity.status(HttpStatus.CREATED)
                                .body(ApiResponseDTO.of(201, "Department created successfully", dept));
        }
}
