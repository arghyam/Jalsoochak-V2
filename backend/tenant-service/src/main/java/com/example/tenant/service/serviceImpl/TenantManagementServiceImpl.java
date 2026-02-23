package com.example.tenant.service.serviceImpl;

import com.example.tenant.config.TenantContext;
import com.example.tenant.dto.CreateDepartmentRequestDTO;
import com.example.tenant.dto.CreateTenantRequestDTO;
import com.example.tenant.dto.DepartmentResponseDTO;
import com.example.tenant.dto.TenantResponseDTO;
import com.example.tenant.kafka.KafkaProducer;
import com.example.tenant.repository.TenantCommonRepository;
import com.example.tenant.repository.TenantSchemaRepository;
import com.example.tenant.service.TenantManagementService;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class TenantManagementServiceImpl implements TenantManagementService {

    private final TenantCommonRepository tenantCommonRepository;
    private final TenantSchemaRepository tenantSchemaRepository;
    private final KafkaProducer kafkaProducer;
    private final ObjectMapper objectMapper;

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

            publishTenantEvent(tenant, "TENANT_CREATED");

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

    private void publishTenantEvent(TenantResponseDTO tenant, String eventType) {
        try {
            int statusInt = "ACTIVE".equalsIgnoreCase(tenant.getStatus()) ? 1 : 0;
            Map<String, Object> event = Map.of(
                    "eventType", eventType,
                    "tenantId", tenant.getId(),
                    "stateCode", tenant.getStateCode(),
                    "title", tenant.getName(),
                    "countryCode", "IN",
                    "status", statusInt
            );
            String json = objectMapper.writeValueAsString(event);
            kafkaProducer.sendMessage(json);
            log.info("Published {} event for tenant [id={}]", eventType, tenant.getId());
        } catch (Exception e) {
            log.error("Failed to publish {} event for tenant [id={}]: {}",
                    eventType, tenant.getId(), e.getMessage());
        }
    }
}
