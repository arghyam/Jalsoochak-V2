package org.arghyam.jalsoochak.tenant.service;

import org.arghyam.jalsoochak.tenant.dto.common.PageResponseDTO;
import org.arghyam.jalsoochak.tenant.dto.request.CreateDepartmentRequestDTO;
import org.arghyam.jalsoochak.tenant.dto.request.CreateTenantRequestDTO;
import org.arghyam.jalsoochak.tenant.dto.request.SetTenantConfigRequestDTO;
import org.arghyam.jalsoochak.tenant.dto.request.UpdateTenantRequestDTO;
import org.arghyam.jalsoochak.tenant.dto.response.DepartmentResponseDTO;
import org.arghyam.jalsoochak.tenant.dto.response.TenantConfigResponseDTO;
import org.arghyam.jalsoochak.tenant.dto.response.TenantResponseDTO;
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
}
