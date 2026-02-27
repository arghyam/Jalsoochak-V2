package org.arghyam.jalsoochak.tenant.service;

import org.arghyam.jalsoochak.tenant.dto.CreateDepartmentRequestDTO;
import org.arghyam.jalsoochak.tenant.dto.CreateTenantRequestDTO;
import org.arghyam.jalsoochak.tenant.dto.DepartmentResponseDTO;
import org.arghyam.jalsoochak.tenant.dto.TenantResponseDTO;

import java.util.List;

public interface TenantManagementService {

    TenantResponseDTO createTenant(CreateTenantRequestDTO request);

    List<DepartmentResponseDTO> getTenantDepartments();

    DepartmentResponseDTO createDepartment(CreateDepartmentRequestDTO request);

    List<TenantResponseDTO> getAllTenants();
}
