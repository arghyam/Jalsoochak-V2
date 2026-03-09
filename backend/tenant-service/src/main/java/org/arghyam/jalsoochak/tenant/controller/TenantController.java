package org.arghyam.jalsoochak.tenant.controller;

import java.util.List;
import java.util.Set;

import org.arghyam.jalsoochak.tenant.dto.common.ApiResponseDTO;
import org.arghyam.jalsoochak.tenant.dto.common.PageResponseDTO;
import org.arghyam.jalsoochak.tenant.dto.request.CreateDepartmentRequestDTO;
import org.arghyam.jalsoochak.tenant.dto.request.CreateTenantRequestDTO;
import org.arghyam.jalsoochak.tenant.dto.request.SetTenantConfigRequestDTO;
import org.arghyam.jalsoochak.tenant.dto.request.UpdateTenantRequestDTO;
import org.arghyam.jalsoochak.tenant.dto.response.DepartmentResponseDTO;
import org.arghyam.jalsoochak.tenant.dto.response.LocationHierarchyResponseDTO;
import org.arghyam.jalsoochak.tenant.dto.response.LocationResponseDTO;
import org.arghyam.jalsoochak.tenant.dto.response.TenantConfigResponseDTO;
import org.arghyam.jalsoochak.tenant.dto.response.TenantResponseDTO;
import org.arghyam.jalsoochak.tenant.enums.TenantConfigKeyEnum;
import org.arghyam.jalsoochak.tenant.service.TenantManagementService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v1/tenants")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Tenant Management", description = "APIs for tenant onboarding, schema provisioning, and tenant management")
public class TenantController {

        private static final int MAX_PAGE_SIZE = 100;

        private final TenantManagementService tenantManagementService;

        /**
         * 1. Create a new tenant
         */
        @Operation(summary = "Create a new tenant", description = "Registers a new tenant in the common schema and provisions a dedicated "
                        + "database schema (tenant_<stateCode>) with all required tables and indexes.")
        @ApiResponses({
                        @ApiResponse(responseCode = "201", description = "Tenant created and schema provisioned successfully"),
                        @ApiResponse(responseCode = "400", description = "Invalid request — missing or malformed fields"),
                        @ApiResponse(responseCode = "409", description = "Tenant with the given state code already exists"),
                        @ApiResponse(responseCode = "500", description = "Internal server error during tenant creation")
        })
        // TODO: Change this to permission / role based authorization for SUPER_ADMIN
        // @PreAuthorize("hasAuthority('tenant.create')")
        @PostMapping
        public ResponseEntity<ApiResponseDTO<TenantResponseDTO>> createTenant(
                        @Valid @RequestBody CreateTenantRequestDTO request) {
                log.info("POST /api/v1/tenants – Creating tenant: {}", request.getName());
                TenantResponseDTO tenant = tenantManagementService.createTenant(request);
                return ResponseEntity.status(HttpStatus.CREATED)
                                .body(ApiResponseDTO.of(201, "Tenant created successfully", tenant));
        }

        /**
         * 2. Get all tenants
         */
        @Operation(summary = "List all tenants with pagination", description = "Returns a paginated list of tenants registered in the common schema, ordered by ID.")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Paginated list of tenants"),
                        @ApiResponse(responseCode = "500", description = "Internal server error")
        })
        // TODO: Change this to permission / role based authorization for SUPER_ADMIN &
        // TENANT_ADMIN
        // @PreAuthorize("hasAuthority('tenant.read')")
        @GetMapping
        public ResponseEntity<ApiResponseDTO<PageResponseDTO<TenantResponseDTO>>> getAllTenants(
                        @Parameter(description = "Page number (0-indexed)", example = "0") @RequestParam(defaultValue = "0") @Min(0) int page,
                        @Parameter(description = "Page size", example = "10") @RequestParam(defaultValue = "10") @Min(1) @Max(MAX_PAGE_SIZE) int size) {
                log.info("GET /api/v1/tenants – page: {}, size: {}", page, size);
                return ResponseEntity.ok(ApiResponseDTO.of(200, "Tenants retrieved successfully",
                                tenantManagementService.getAllTenants(page, size)));
        }

        /**
         * 3. Update a tenant
         */
        @Operation(summary = "Update tenant", description = "Updates the status of an existing tenant identified by tenantId.")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Tenant updated successfully"),
                        @ApiResponse(responseCode = "400", description = "Tenant updation failed"),
                        @ApiResponse(responseCode = "404", description = "Tenant with given tenantId does not exist"),
                        @ApiResponse(responseCode = "500", description = "Internal server error")
        })
        // TODO: Change this to permission / role based authorization for SUPER_ADMIN
        // @PreAuthorize("hasAuthority('tenant.update')")
        @PutMapping("/{tenantId}")
        public ResponseEntity<ApiResponseDTO<TenantResponseDTO>> updateTenant(
                        @PathVariable Integer tenantId,
                        @Valid @RequestBody UpdateTenantRequestDTO request) {
                log.info("PUT /api/v1/tenants/{}", tenantId);
                TenantResponseDTO updated = tenantManagementService.updateTenant(tenantId, request);
                return ResponseEntity.ok(ApiResponseDTO.of(200, "Tenant updated successfully", updated));
        }

        /**
         * 4. Deactivate a tenant
         */
        @Operation(summary = "Deactivate (soft-delete) a tenant", description = "Sets the tenant status to INACTIVE for the given tenantId.")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Tenant deactivated successfully"),
                        @ApiResponse(responseCode = "400", description = "Tenant deactivation failed"),
                        @ApiResponse(responseCode = "404", description = "Tenant with given tenantId does not exist"),
                        @ApiResponse(responseCode = "500", description = "Internal server error")
        })
        // TODO: Change this to permission / role based authorization for SUPER_ADMIN
        // @PreAuthorize("hasAuthority('tenant.delete')")
        @PutMapping("/{tenantId}/deactivate")
        public ResponseEntity<ApiResponseDTO<Void>> deactivateTenant(@PathVariable Integer tenantId) {
                log.info("PUT /api/v1/tenants/{}/deactivate", tenantId);
                tenantManagementService.deactivateTenant(tenantId);
                return ResponseEntity.ok(ApiResponseDTO.of(200, "Tenant deactivated successfully"));
        }

        /**
         * 5. Get all configurations for a tenant
         */
        @Operation(summary = "Get the configurations for a tenant", description = "Retrieves either all or the selected configuration key-value pairs for a specific tenant in a Map format.")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Tenant configurations retrieved successfully"),
                        @ApiResponse(responseCode = "404", description = "Tenant not found"),
                        @ApiResponse(responseCode = "500", description = "Internal server error")
        })
        // TODO: Change this to permission based authorization
        // @PreAuthorize("hasAuthority('tenant.config.read')")
        @GetMapping("/{tenantId}/config")
        public ResponseEntity<ApiResponseDTO<TenantConfigResponseDTO>> getTenantConfigs(
                        @PathVariable Integer tenantId,
                        @Parameter(description = "Optional set of configuration keys to retrieve. If not provided, all configurations are returned.", example = "KEY1, KEY2") @RequestParam(required = false) Set<TenantConfigKeyEnum> keys) {
                log.info("GET /api/v1/tenants/{}/config with keys: {}", tenantId, keys);
                return ResponseEntity.ok(ApiResponseDTO.of(200, "Tenant configurations retrieved successfully",
                                tenantManagementService.getTenantConfigs(tenantId, keys)));
        }

        /**
         * 6. Set or update multiple configurations for a tenant
         */
        @Operation(summary = "Set or update multiple tenant configurations", description = "Batch updates or creates configurations for the specified tenant using a Map structure.")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Configurations set successfully"),
                        @ApiResponse(responseCode = "404", description = "Tenant not found"),
                        @ApiResponse(responseCode = "500", description = "Internal server error")
        })
        // TODO: Change this to permission based authorization
        // @PreAuthorize("hasAuthority('tenant.config.update') or
        // hasAuthority('tenant.config.create')")
        @PutMapping("/{tenantId}/config")
        public ResponseEntity<ApiResponseDTO<TenantConfigResponseDTO>> setTenantConfigs(
                        @PathVariable Integer tenantId,
                        @Valid @RequestBody SetTenantConfigRequestDTO request) {
                log.info("PUT /api/v1/tenants/{}/config mapping received", tenantId);
                return ResponseEntity.ok(ApiResponseDTO.of(200, "Tenant configurations set successfully",
                                tenantManagementService.setTenantConfigs(tenantId, request)));
        }

        /**
         * 7. Get tenant location hierarchy by hierarchy type
         */
        @Operation(summary = "Get location hierarchy configuration for a tenant", description = "Retrieves the location hierarchy structure (levels) for the specified hierarchy type (LGD or DEPARTMENT). ")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Location hierarchy configuration retrieved successfully"),
                        @ApiResponse(responseCode = "400", description = "Invalid hierarchy type or tenant could not be resolved"),
                        @ApiResponse(responseCode = "404", description = "Hierarchy configuration not found for the tenant"),
                        @ApiResponse(responseCode = "500", description = "Internal server error")
        })
        @GetMapping("/{tenantId}/location-hierarchy/{hierarchyType}")
        public ResponseEntity<ApiResponseDTO<LocationHierarchyResponseDTO>> getTenantLocationHierarchy(
                        @PathVariable Integer tenantId,
                        @Parameter(description = "Hierarchy type: LGD or DEPARTMENT", example = "LGD") @PathVariable String hierarchyType) {
                log.info("GET /api/v1/tenants/{}/location-hierarchy/{}", tenantId, hierarchyType);
                LocationHierarchyResponseDTO hierarchy = tenantManagementService.getLocationHierarchy(tenantId,
                                hierarchyType);
                return ResponseEntity
                                .ok(ApiResponseDTO.of(200, "Location hierarchy retrieved successfully", hierarchy));
        }

        /**
         * 8. Get child locations by parent id and hierarchy type
         */
        @Operation(summary = "Get child locations by parent ID", description = "Fetches all child locations under the specified parent location in the given hierarchy type. "
                        + "Pass parentId as 0 to fetch root-level locations (where parent_id IS NULL).")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Child locations retrieved successfully"),
                        @ApiResponse(responseCode = "400", description = "Invalid hierarchy type or tenant could not be resolved"),
                        @ApiResponse(responseCode = "500", description = "Internal server error")
        })
        @GetMapping("/{tenantId}/locations/{hierarchyType}/children/{parentId}")
        public ResponseEntity<ApiResponseDTO<List<LocationResponseDTO>>> getLocationChildren(
                        @PathVariable Integer tenantId,
                        @Parameter(description = "Hierarchy type: LGD or DEPARTMENT", example = "LGD") @PathVariable String hierarchyType,
                        @Parameter(description = "Parent location ID (use 0 for root-level locations)", example = "1") @PathVariable Integer parentId) {
                log.info("GET /api/v1/tenants/{}/locations/{}/children/{}", tenantId, hierarchyType, parentId);
                Integer actualParentId = parentId.equals(0) ? null : parentId;
                List<LocationResponseDTO> children = tenantManagementService.getLocationChildren(tenantId,
                                hierarchyType, actualParentId);
                return ResponseEntity.ok(ApiResponseDTO.of(200, "Child locations retrieved successfully", children));
        }

        /**
         * 9. Get departments for the current tenant
         */
        @Operation(summary = "Get departments for the current tenant", description = "Fetches the department hierarchy from the tenant-specific schema. "
                        + "Requires the X-Tenant-Code header to be set by the API gateway.")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "List of departments for the resolved tenant"),
                        @ApiResponse(responseCode = "400", description = "Tenant could not be resolved — missing X-Tenant-Code header"),
                        @ApiResponse(responseCode = "500", description = "Internal server error")
        })
        // TODO: Change this to permission / role based authorization for SUPER_ADMIN &
        // TENANT_ADMIN
        // @PreAuthorize("hasAuthority('tenant.department.read')")
        @GetMapping("/departments")
        public ResponseEntity<ApiResponseDTO<List<DepartmentResponseDTO>>> getTenantDepartments() {
                log.info("GET /api/v1/tenants/departments");
                return ResponseEntity.ok(ApiResponseDTO.of(200, "Departments retrieved successfully",
                                tenantManagementService.getTenantDepartments()));
        }

        /**
         * 10. Create a department for the current tenant
         */
        @Operation(summary = "Create a department for the current tenant", description = "Inserts a new department into the tenant-specific schema's department_location_master_table. "
                        + "Requires the X-Tenant-Code header to identify the target tenant schema.")
        @ApiResponses({
                        @ApiResponse(responseCode = "201", description = "Department created successfully"),
                        @ApiResponse(responseCode = "400", description = "Invalid request or tenant could not be resolved"),
                        @ApiResponse(responseCode = "500", description = "Internal server error")
        })
        // TODO: Change this to permission / role based authorization for SUPER_ADMIN &
        // TENANT_ADMIN
        // @PreAuthorize("hasAuthority('tenant.department.create')")
        @PostMapping("/departments")
        public ResponseEntity<ApiResponseDTO<DepartmentResponseDTO>> createDepartment(
                        @Valid @RequestBody CreateDepartmentRequestDTO request) {
                log.info("POST /api/v1/tenants/departments – Creating department: {}", request.getTitle());
                DepartmentResponseDTO dept = tenantManagementService.createDepartment(request);
                return ResponseEntity.status(HttpStatus.CREATED)
                                .body(ApiResponseDTO.of(201, "Department created successfully", dept));
        }
}
