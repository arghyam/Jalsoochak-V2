package com.example.tenant.service;

import com.example.tenant.config.TenantContext;
import com.example.tenant.dto.CreateTenantRequest;
import com.example.tenant.dto.DepartmentResponse;
import com.example.tenant.dto.TenantResponse;
import com.example.tenant.repository.TenantCommonRepository;
import com.example.tenant.repository.TenantSchemaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TenantManagementServiceImpl implements TenantManagementService {

    private final TenantCommonRepository tenantCommonRepository;
    private final TenantSchemaRepository tenantSchemaRepository;

    @Override
    @Transactional
    public TenantResponse createTenant(CreateTenantRequest request) {
        log.info("Creating tenant – stateCode: {}, name: {}", request.getStateCode(), request.getName());

        validateCreateRequest(request);

        tenantCommonRepository.findByStateCode(request.getStateCode()).ifPresent(existing -> {
            throw new IllegalStateException(
                    "Tenant with state code '" + request.getStateCode() + "' already exists");
        });

        TenantResponse tenant = tenantCommonRepository.createTenant(request);
        log.info("Tenant record created in common_schema with id: {}", tenant.getId());

        String schemaName = "tenant_" + request.getStateCode().toLowerCase();
        tenantCommonRepository.provisionTenantSchema(schemaName);
        log.info("Tenant schema '{}' provisioned successfully", schemaName);

        return tenant;
    }

    @Override
    public List<DepartmentResponse> getTenantDepartments() {
        String schemaName = TenantContext.getSchema();
        if (schemaName == null || schemaName.isBlank()) {
            throw new IllegalArgumentException(
                    "Tenant could not be resolved. Ensure the X-Tenant-Code header is set by the gateway.");
        }
        log.info("Fetching departments from schema: {}", schemaName);
        return tenantSchemaRepository.getDepartments(schemaName);
    }

    @Override
    public List<TenantResponse> getAllTenants() {
        return tenantCommonRepository.findAll();
    }

    private void validateCreateRequest(CreateTenantRequest request) {
        if (request.getStateCode() == null || request.getStateCode().isBlank()) {
            throw new IllegalArgumentException("State code is required");
        }
        if (request.getLgdCode() == null) {
            throw new IllegalArgumentException("LGD code is required");
        }
        if (request.getName() == null || request.getName().isBlank()) {
            throw new IllegalArgumentException("Tenant name is required");
        }
        if (!request.getStateCode().matches("^[A-Za-z]{2,10}$")) {
            throw new IllegalArgumentException("State code must be 2-10 alphabetic characters");
        }
    }
}
