package org.arghyam.jalsoochak.tenant.controller;

import java.util.Set;
import org.arghyam.jalsoochak.tenant.enums.TenantConfigKeyEnum;

import org.arghyam.jalsoochak.tenant.dto.ApiResponseDTO;
import org.arghyam.jalsoochak.tenant.dto.CreateDepartmentRequestDTO;
import org.arghyam.jalsoochak.tenant.dto.CreateTenantRequestDTO;
import org.arghyam.jalsoochak.tenant.dto.DepartmentResponseDTO;
import org.arghyam.jalsoochak.tenant.dto.PageResponseDTO;
import org.arghyam.jalsoochak.tenant.dto.SetTenantConfigRequestDTO;
import org.arghyam.jalsoochak.tenant.dto.TenantConfigResponseDTO;
import org.arghyam.jalsoochak.tenant.dto.TenantResponseDTO;
import org.arghyam.jalsoochak.tenant.dto.UpdateTenantRequestDTO;
import org.arghyam.jalsoochak.tenant.service.TenantManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
import org.springframework.web.bind.annotation.RequestParam;
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
        // TODO: Change this to permission / role based authorization for SUPER_ADMIN
        // @PreAuthorize("hasAuthority('tenant.create')")
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
        @Operation(summary = "List all tenants with pagination", description = "Returns a paginated list of tenants registered in the common schema, ordered by ID.")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Paginated list of tenants", content = @Content(schema = @Schema(implementation = PageResponseDTO.class))),
                        @ApiResponse(responseCode = "500", description = "Internal server error")
        })
        // TODO: Change this to permission / role based authorization for SUPER_ADMIN &
        // TENANT_ADMIN
        // @PreAuthorize("hasAuthority('tenant.read')")
        @PreAuthorize("permitAll")
        @GetMapping
        public ResponseEntity<ApiResponseDTO<PageResponseDTO<TenantResponseDTO>>> getAllTenants(
                        @Parameter(description = "Page number (0-indexed)", example = "0") @RequestParam(defaultValue = "0") int page,
                        @Parameter(description = "Page size", example = "10") @RequestParam(defaultValue = "10") int size) {
                log.info("GET /api/v1/tenants – page: {}, size: {}", page, size);
                return ResponseEntity.ok(ApiResponseDTO.of(200, "Tenants retrieved successfully",
                                tenantManagementService.getAllTenants(page, size)));
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
        // TODO: Change this to permission / role based authorization for SUPER_ADMIN
        // @PreAuthorize("hasAuthority('tenant.update')")
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
        // TODO: Change this to permission / role based authorization for SUPER_ADMIN
        // @PreAuthorize("hasAuthority('tenant.delete')")
        @PreAuthorize("permitAll")
        @PutMapping("/{tenantId}/deactivate")
        public ResponseEntity<ApiResponseDTO<Void>> deactivateTenant(@PathVariable Integer tenantId) {
                log.info("PUT /api/v1/tenants/{}/deactivate", tenantId);
                tenantManagementService.deactivateTenant(tenantId);
                return ResponseEntity.ok(ApiResponseDTO.of(200, "Tenant deactivated successfully"));
        }

        /**
         * 5. API for getting all configurations for a tenant - accessible by super
         * admin and tenant admin
         */
        @Operation(summary = "Get all configurations for a tenant", description = "Retrieves all active configuration key-value pairs for a specific tenant in a Map format.")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Tenant configurations retrieved successfully"),
                        @ApiResponse(responseCode = "404", description = "Tenant not found"),
                        @ApiResponse(responseCode = "500", description = "Internal server error")
        })
        // TODO: Change this to permission based authorization
        // @PreAuthorize("hasAuthority('tenant.config.read')")
        @PreAuthorize("permitAll")
        @GetMapping("/{tenantId}/config")
        public ResponseEntity<ApiResponseDTO<TenantConfigResponseDTO>> getTenantConfigs(
                        @PathVariable Integer tenantId,
                        @Parameter(
                                description = "Optional set of configuration keys to retrieve. If not provided, all configurations are returned.",
                                example = "KEY1, KEY2"
                        )
                        @RequestParam(required = false) Set<TenantConfigKeyEnum> keys) {
                log.info("GET /api/v1/tenants/{}/config with keys: {}", tenantId, keys);
                return ResponseEntity.ok(ApiResponseDTO.of(200, "Tenant configurations retrieved successfully",
                                tenantManagementService.getTenantConfigs(tenantId, keys)));
        }

        /**
         * 6. API for setting or updating multiple configurations for a tenant -
         * accessible by super admin and tenant admin
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
        @PreAuthorize("permitAll")
        @PutMapping("/{tenantId}/config")
        public ResponseEntity<ApiResponseDTO<TenantConfigResponseDTO>> setTenantConfigs(
                        @PathVariable Integer tenantId,
                        @Valid @RequestBody SetTenantConfigRequestDTO request) {
                log.info("PUT /api/v1/tenants/{}/config mapping received", tenantId);
                return ResponseEntity.ok(ApiResponseDTO.of(200, "Tenant configurations set successfully",
                                tenantManagementService.setTenantConfigs(tenantId, request)));
        }

        /**
         * 7. API for getting departments for the current tenant - accessible by super
         * admin and tenant admin
         */
        @Operation(summary = "Get departments for the current tenant", description = "Fetches the department hierarchy from the tenant-specific schema. "
                        + "Requires the X-Tenant-Code header to be set by the API gateway.")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "List of departments for the resolved tenant", content = @Content(array = @ArraySchema(schema = @Schema(implementation = DepartmentResponseDTO.class)))),
                        @ApiResponse(responseCode = "400", description = "Tenant could not be resolved — missing X-Tenant-Code header"),
                        @ApiResponse(responseCode = "500", description = "Internal server error")
        })
        // TODO: Change this to permission / role based authorization for SUPER_ADMIN &
        // TENANT_ADMIN
        // @PreAuthorize("hasAuthority('tenant.department.read')")
        @PreAuthorize("permitAll")
        @GetMapping("/departments")
        public ResponseEntity<ApiResponseDTO<List<DepartmentResponseDTO>>> getTenantDepartments() {
                log.info("GET /api/v1/tenants/departments");
                return ResponseEntity.ok(ApiResponseDTO.of(200, "Departments retrieved successfully",
                                tenantManagementService.getTenantDepartments()));
        }

        /**
         * 8. API for creating a department for the current tenant - accessible by super
         * admin and tenant admin
         */
        @Operation(summary = "Create a department for the current tenant", description = "Inserts a new department into the tenant-specific schema's department_location_master_table. "
                        + "Requires the X-Tenant-Code header to identify the target tenant schema.")
        @ApiResponses({
                        @ApiResponse(responseCode = "201", description = "Department created successfully", content = @Content(schema = @Schema(implementation = DepartmentResponseDTO.class))),
                        @ApiResponse(responseCode = "400", description = "Invalid request or tenant could not be resolved"),
                        @ApiResponse(responseCode = "500", description = "Internal server error")
        })
        // TODO: Change this to permission / role based authorization for SUPER_ADMIN &
        // TENANT_ADMIN
        // @PreAuthorize("hasAuthority('tenant.department.create')")
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
