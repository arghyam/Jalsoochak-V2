package org.arghyam.jalsoochak.tenant.service.serviceImpl;

import org.arghyam.jalsoochak.tenant.config.TenantContext;
import org.arghyam.jalsoochak.tenant.dto.CreateDepartmentRequestDTO;
import org.arghyam.jalsoochak.tenant.dto.CreateTenantRequestDTO;
import org.arghyam.jalsoochak.tenant.dto.DepartmentResponseDTO;
import org.arghyam.jalsoochak.tenant.dto.PageResponseDTO;
import org.arghyam.jalsoochak.tenant.dto.TenantResponseDTO;
import org.arghyam.jalsoochak.tenant.dto.UpdateTenantRequestDTO;
import org.arghyam.jalsoochak.tenant.kafka.KafkaProducer;
import org.arghyam.jalsoochak.tenant.repository.TenantCommonRepository;
import org.arghyam.jalsoochak.tenant.repository.TenantSchemaRepository;
import org.arghyam.jalsoochak.tenant.service.TenantManagementService;
import org.arghyam.jalsoochak.tenant.exception.ResourceNotFoundException;
import org.arghyam.jalsoochak.tenant.util.SecurityUtils;
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
            String uuid = SecurityUtils.getCurrentUserUuid();
            Integer currentUserId = tenantCommonRepository.findUserIdByUuid(uuid).orElse(null);

            TenantResponseDTO tenant = tenantCommonRepository.createTenant(request, currentUserId)
                    .orElseThrow(() -> new RuntimeException("Tenant creation failed – no record returned"));
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
    @Transactional
    public TenantResponseDTO updateTenant(Integer tenantId, UpdateTenantRequestDTO request) {
        log.info("Updating tenant [id={}]", tenantId);

        tenantCommonRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Tenant with tenantId " + tenantId + " does not exist"));

        if ("INACTIVE".equalsIgnoreCase(request.getStatus())) {
            throw new IllegalArgumentException(
                    "Cannot deactivate tenant via this endpoint. Use the deactivateTenant endpoint instead.");
        }

        try {
            String uuid = SecurityUtils.getCurrentUserUuid();
            Integer currentUserId = tenantCommonRepository.findUserIdByUuid(uuid).orElse(null);

            TenantResponseDTO updated = tenantCommonRepository.updateTenant(tenantId, request, currentUserId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Tenant with tenantId " + tenantId + " does not exist"));
            log.info("Tenant [id={}] updated successfully", tenantId);
            publishTenantEvent(updated, "TENANT_UPDATED");
            return updated;
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to update tenant [id={}]", tenantId, e);
            throw new RuntimeException("Tenant updation failed", e);
        }
    }

    @Override
    @Transactional
    public void deactivateTenant(Integer tenantId) {
        log.info("Deactivating tenant [id={}]", tenantId);

        tenantCommonRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Tenant with tenantId " + tenantId + " does not exist"));

        try {
            String uuid = SecurityUtils.getCurrentUserUuid();
            Integer currentUserId = tenantCommonRepository.findUserIdByUuid(uuid).orElse(null);

            tenantCommonRepository.deactivateTenant(tenantId, currentUserId);
            log.info("Tenant [id={}] deactivated successfully", tenantId);

            // Fetch final state to publish event
            tenantCommonRepository.findById(tenantId)
                    .ifPresent(tenant -> publishTenantEvent(tenant, "TENANT_DEACTIVATED"));
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to deactivate tenant [id={}]", tenantId, e);
            throw new RuntimeException("Tenant deactivation failed", e);
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
            String currentUuid = SecurityUtils.getCurrentUserUuid();
            Integer currentUserId = tenantCommonRepository.findUserIdByUuid(currentUuid).orElse(null);
            return tenantSchemaRepository.createDepartment(schemaName, request, currentUserId)
                    .orElseThrow(() -> new RuntimeException("Department creation failed – no record returned"));
        } catch (Exception e) {
            log.error("Failed to create department in schema: {}", schemaName, e);
            throw new RuntimeException("Department creation failed: " + e.getMessage(), e);
        }
    }

    @Override
    public PageResponseDTO<TenantResponseDTO> getAllTenants(int page, int size) {
        try {
            int offset = page * size;
            List<TenantResponseDTO> tenants = tenantCommonRepository.findAll(size, offset);
            long totalElements = tenantCommonRepository.countAllTenants();

            return PageResponseDTO.of(tenants, totalElements, page, size);
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
                    "status", statusInt);
            String json = objectMapper.writeValueAsString(event);
            kafkaProducer.sendMessage(json);
            log.info("Published {} event for tenant [id={}]", eventType, tenant.getId());
        } catch (Exception e) {
            log.error("Failed to publish {} event for tenant [id={}]: {}",
                    eventType, tenant.getId(), e.getMessage());
        }
    }

}
