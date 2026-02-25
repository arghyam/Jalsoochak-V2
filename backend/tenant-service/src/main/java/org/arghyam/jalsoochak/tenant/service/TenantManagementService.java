package org.arghyam.jalsoochak.tenant.service;

import java.util.List;

import org.arghyam.jalsoochak.tenant.dto.CreateDepartmentRequestDTO;
import org.arghyam.jalsoochak.tenant.dto.CreateTenantRequestDTO;
import org.arghyam.jalsoochak.tenant.dto.DepartmentResponseDTO;
import org.arghyam.jalsoochak.tenant.dto.PageResponseDTO;
import org.arghyam.jalsoochak.tenant.dto.TenantResponseDTO;
import org.arghyam.jalsoochak.tenant.dto.UpdateTenantRequestDTO;

public interface TenantManagementService {

    TenantResponseDTO createTenant(CreateTenantRequestDTO request);

    TenantResponseDTO updateTenant(Integer tenantId, UpdateTenantRequestDTO request);

    void deactivateTenant(Integer tenantId);

    List<DepartmentResponseDTO> getTenantDepartments();

    DepartmentResponseDTO createDepartment(CreateDepartmentRequestDTO request);

    PageResponseDTO<TenantResponseDTO> getAllTenants(int page, int size);
}
