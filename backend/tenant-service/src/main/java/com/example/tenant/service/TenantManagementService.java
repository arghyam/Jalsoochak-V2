package com.example.tenant.service;

import com.example.tenant.dto.CreateDepartmentRequestDTO;
import com.example.tenant.dto.CreateTenantRequestDTO;
import com.example.tenant.dto.DepartmentResponseDTO;
import com.example.tenant.dto.TenantResponseDTO;

import java.util.List;

public interface TenantManagementService {

    TenantResponseDTO createTenant(CreateTenantRequestDTO request);

    List<DepartmentResponseDTO> getTenantDepartments();

    DepartmentResponseDTO createDepartment(CreateDepartmentRequestDTO request);

    List<TenantResponseDTO> getAllTenants();
}
