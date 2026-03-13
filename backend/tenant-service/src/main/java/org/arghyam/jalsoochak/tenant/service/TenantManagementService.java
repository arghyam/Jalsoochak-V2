package org.arghyam.jalsoochak.tenant.service;

import org.arghyam.jalsoochak.tenant.dto.common.PageResponseDTO;
import org.arghyam.jalsoochak.tenant.dto.request.CreateDepartmentRequestDTO;
import org.arghyam.jalsoochak.tenant.dto.request.CreateTenantRequestDTO;
import org.arghyam.jalsoochak.tenant.dto.request.SetTenantConfigRequestDTO;
import org.arghyam.jalsoochak.tenant.dto.request.UpdateTenantRequestDTO;
import org.arghyam.jalsoochak.tenant.dto.response.DepartmentResponseDTO;
import org.arghyam.jalsoochak.tenant.dto.response.TenantConfigResponseDTO;
import org.arghyam.jalsoochak.tenant.dto.response.TenantResponseDTO;
import org.arghyam.jalsoochak.tenant.dto.response.LocationResponseDTO;
import org.arghyam.jalsoochak.tenant.dto.response.LocationHierarchyEditConstraintsResponseDTO;
import org.arghyam.jalsoochak.tenant.dto.response.LocationHierarchyResponseDTO;
import org.arghyam.jalsoochak.tenant.dto.internal.LocationLevelConfigDTO;
import org.arghyam.jalsoochak.tenant.enums.TenantConfigKeyEnum;

import java.util.List;
import java.util.Set;

/**
 * Service for managing tenants and their configurations.
 */
public interface TenantManagementService {

    /**
     * Creates a new tenant.
     * 
     * @param request Tenant creation request.
     * @return Created tenant response.
     */
    TenantResponseDTO createTenant(CreateTenantRequestDTO request);

    /**
     * Updates an existing tenant.
     * 
     * @param tenantId ID of the tenant.
     * @param request  Tenant update request.
     * @return Updated tenant response.
     */
    TenantResponseDTO updateTenant(Integer tenantId, UpdateTenantRequestDTO request);

    /**
     * Deactivates a tenant.
     * 
     * @param tenantId ID of the tenant.
     */
    void deactivateTenant(Integer tenantId);

    /**
     * Gets all departments of a tenant.
     * 
     * @return List of department responses.
     */
    List<DepartmentResponseDTO> getTenantDepartments();

    /**
     * Creates a new department.
     * 
     * @param request Department creation request.
     * @return Created department response.
     */
    DepartmentResponseDTO createDepartment(CreateDepartmentRequestDTO request);

    /**
     * Gets all tenants.
     * 
     * @param page Page number.
     * @param size Page size.
     * @return Page of tenants.
     */
    PageResponseDTO<TenantResponseDTO> getAllTenants(int page, int size);

    /**
     * Gets tenant configurations.
     * 
     * @param tenantId ID of the tenant.
     * @param keys     Optional filter for configuration keys.
     * @return Tenant configurations response.
     */
    TenantConfigResponseDTO getTenantConfigs(Integer tenantId, Set<TenantConfigKeyEnum> keys);

    /**
     * Sets tenant configurations.
     * 
     * @param tenantId ID of the tenant.
     * @param request  Tenant configuration request.
     * @return Tenant configurations response.
     */
    TenantConfigResponseDTO setTenantConfigs(Integer tenantId, SetTenantConfigRequestDTO request);

    /**
     * Gets the location hierarchy configuration for a tenant.
     * 
     * @param hierarchyType Type of hierarchy: LGD or DEPARTMENT
     * @return Location hierarchy configuration with levels and names.
     */
    LocationHierarchyResponseDTO getLocationHierarchy(Integer tenantId, String hierarchyType);

    /**
     * Gets child locations by parent ID.
     *
     * @param hierarchyType Type of hierarchy: LGD or DEPARTMENT
     * @param parentId      Parent location ID (null for root-level locations)
     * @return List of child location records.
     */
    List<LocationResponseDTO> getLocationChildren(Integer tenantId, String hierarchyType, Integer parentId);

    /**
     * Returns edit constraints for the given location hierarchy type.
     * Tells the caller whether structural changes (add/remove levels) are permitted,
     * based on whether seeded data exists in the master table.
     *
     * @param tenantId      ID of the tenant.
     * @param hierarchyType LGD or DEPARTMENT
     * @return Edit constraints response.
     */
    LocationHierarchyEditConstraintsResponseDTO getLocationHierarchyEditConstraints(
            Integer tenantId, String hierarchyType);

    /**
     * Updates the location hierarchy for the given type.
     * Internally decides the update strategy:
     * - If no seeded data exists: full structural update (delete + insert).
     * - If seeded data exists and only level names changed: updates names in-place.
     * - If seeded data exists and structure differs: throws LocationHierarchyStructureLockedException (409).
     *
     * @param tenantId      ID of the tenant.
     * @param hierarchyType LGD or DEPARTMENT
     * @param levels        New hierarchy level definitions.
     * @return Updated location hierarchy.
     */
    LocationHierarchyResponseDTO updateLocationHierarchy(
            Integer tenantId, String hierarchyType, List<LocationLevelConfigDTO> levels);

}
