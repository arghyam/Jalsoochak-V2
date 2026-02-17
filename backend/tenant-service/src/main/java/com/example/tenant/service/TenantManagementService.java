package com.example.tenant.service;

import com.example.tenant.dto.CreateTenantRequest;
import com.example.tenant.dto.DepartmentResponse;
import com.example.tenant.dto.TenantResponse;

import java.util.List;

public interface TenantManagementService {

    TenantResponse createTenant(CreateTenantRequest request);

    List<DepartmentResponse> getTenantDepartments();

    List<TenantResponse> getAllTenants();
}
