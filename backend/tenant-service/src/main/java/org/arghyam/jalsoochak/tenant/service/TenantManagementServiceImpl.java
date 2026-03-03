package org.arghyam.jalsoochak.tenant.service;

import org.arghyam.jalsoochak.tenant.config.TenantContext;
import org.arghyam.jalsoochak.tenant.dto.CreateDepartmentRequestDTO;
import org.arghyam.jalsoochak.tenant.dto.CreateTenantRequestDTO;
import org.arghyam.jalsoochak.tenant.dto.DepartmentResponseDTO;
import org.arghyam.jalsoochak.tenant.dto.TenantResponseDTO;
import org.arghyam.jalsoochak.tenant.repository.TenantCommonRepository;
import org.arghyam.jalsoochak.tenant.repository.TenantSchemaRepository;
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
    public TenantResponseDTO createTenant(CreateTenantRequestDTO request) {
        log.info("Creating tenant – stateCode: {}, name: {}", request.getStateCode(), request.getName());

        tenantCommonRepository.findByStateCode(request.getStateCode()).ifPresent(existing -> {
            throw new IllegalStateException(
                    "Tenant with state code '" + request.getStateCode() + "' already exists");
        });

        try {
            TenantResponseDTO tenant = tenantCommonRepository.createTenant(request);
            log.info("Tenant record created in common_schema with id: {}", tenant.getId());

            String schemaName = "tenant_" + request.getStateCode().toLowerCase();
            tenantCommonRepository.provisionTenantSchema(schemaName);
            log.info("Tenant schema '{}' provisioned successfully", schemaName);

            return tenant;
        } catch (IllegalArgumentException | IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to create tenant with stateCode: {}", request.getStateCode(), e);
            throw new RuntimeException("Tenant creation failed: " + e.getMessage(), e);
        }
    }

    @Override
    public List<DepartmentResponseDTO> getTenantDepartments() {
        String schemaName = TenantContext.getSchema();
        if (schemaName == null || schemaName.isBlank()) {
            throw new IllegalArgumentException(
                    "Tenant could not be resolved. Ensure the X-Tenant-Code header is set by the gateway.");
        }
        log.info("Fetching departments from schema: {}", schemaName);
        try {
            return tenantSchemaRepository.getDepartments(schemaName);
        } catch (Exception e) {
            log.error("Failed to fetch departments for schema: {}", schemaName, e);
            throw new RuntimeException("Failed to fetch departments: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public DepartmentResponseDTO createDepartment(CreateDepartmentRequestDTO request) {
        String schemaName = TenantContext.getSchema();
        if (schemaName == null || schemaName.isBlank()) {
            throw new IllegalArgumentException(
                    "Tenant could not be resolved. Ensure the X-Tenant-Code header is set.");
        }
        log.info("Creating department in schema: {}", schemaName);
        try {
            return tenantSchemaRepository.createDepartment(schemaName, request);
        } catch (Exception e) {
            log.error("Failed to create department in schema: {}", schemaName, e);
            throw new RuntimeException("Department creation failed: " + e.getMessage(), e);
        }
    }

    @Override
    public List<TenantResponseDTO> getAllTenants() {
        try {
            return tenantCommonRepository.findAll();
        } catch (Exception e) {
            log.error("Failed to fetch tenants", e);
            throw new RuntimeException("Failed to fetch tenants: " + e.getMessage(), e);
        }
    }
}
