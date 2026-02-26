package org.arghyam.jalsoochak.tenant.service.serviceImpl;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.arghyam.jalsoochak.tenant.config.TenantContext;
import org.arghyam.jalsoochak.tenant.dto.CreateDepartmentRequestDTO;
import org.arghyam.jalsoochak.tenant.dto.CreateTenantRequestDTO;
import org.arghyam.jalsoochak.tenant.dto.DepartmentResponseDTO;
import org.arghyam.jalsoochak.tenant.dto.InternalConfigDTO;
import org.arghyam.jalsoochak.tenant.dto.LanguageConfigDTO;
import org.arghyam.jalsoochak.tenant.dto.PageResponseDTO;
import org.arghyam.jalsoochak.tenant.dto.SetTenantConfigRequestDTO;
import org.arghyam.jalsoochak.tenant.dto.TenantConfigResponseDTO;
import org.arghyam.jalsoochak.tenant.dto.TenantLanguageConfigDTO;
import org.arghyam.jalsoochak.tenant.dto.TenantLocationHierarchyConfigDTO;
import org.arghyam.jalsoochak.tenant.dto.TenantResponseDTO;
import org.arghyam.jalsoochak.tenant.dto.UpdateTenantRequestDTO;
import org.arghyam.jalsoochak.tenant.enums.RegionTypeEnum;
import org.arghyam.jalsoochak.tenant.enums.TenantConfigKeyEnum;
import org.arghyam.jalsoochak.tenant.enums.TenantConfigKeyEnum.ConfigType;
import org.arghyam.jalsoochak.tenant.exception.ResourceNotFoundException;
import org.arghyam.jalsoochak.tenant.kafka.KafkaProducer;
import org.arghyam.jalsoochak.tenant.repository.TenantCommonRepository;
import org.arghyam.jalsoochak.tenant.repository.TenantSchemaRepository;
import org.arghyam.jalsoochak.tenant.service.TenantManagementService;
import org.arghyam.jalsoochak.tenant.util.SecurityUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class TenantManagementServiceImpl implements TenantManagementService {

    private final TenantCommonRepository tenantCommonRepository;
    private final TenantSchemaRepository tenantSchemaRepository;
    private final KafkaProducer kafkaProducer;
    private final ObjectMapper objectMapper;
    private final StringRedisTemplate redisTemplate;

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

            cacheTenantInRedis(tenant, schemaName);
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

    @Override
    public TenantConfigResponseDTO getTenantConfigs(Integer tenantId, Set<TenantConfigKeyEnum> keys) {
        log.info("Fetching tenant configurations [id={}, keys={}]", tenantId, keys);
        TenantResponseDTO tenant = tenantCommonRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Tenant with tenantId " + tenantId + " does not exist"));

        // Normalize keys: if null or empty, fetch all available keys
        Set<TenantConfigKeyEnum> effectiveKeys = (keys == null || keys.isEmpty())
                ? EnumSet.allOf(TenantConfigKeyEnum.class)
                : keys;

        try {
            // 1. Fetch generic configs from common_schema
            List<InternalConfigDTO> configs = tenantCommonRepository.findConfigsByTenantId(tenantId);
            Map<TenantConfigKeyEnum, String> configMap = new HashMap<>();

            for (InternalConfigDTO cfg : configs) {
                try {
                    TenantConfigKeyEnum key = TenantConfigKeyEnum.valueOf(cfg.getConfigKey());
                    if (effectiveKeys.contains(key)) {
                        configMap.put(key, cfg.getConfigValue());
                    }
                } catch (IllegalArgumentException e) {
                    log.error("Invalid tenant config key [key={}]", cfg.getConfigKey(), e);
                    throw new RuntimeException("Invalid tenant config key: " + cfg.getConfigKey());
                }
            }

            // 2. Fetch specialized configs from tenant-specific schema
            String schemaName = "tenant_" + tenant.getStateCode().toLowerCase();

            // Add Languages
            if (effectiveKeys.contains(TenantConfigKeyEnum.SUPPORTED_LANGUAGES)) {
                List<LanguageConfigDTO> langs = tenantSchemaRepository.getSupportedLanguages(schemaName);
                if (langs != null && langs.size() > 0) {
                    TenantLanguageConfigDTO languageConfig = TenantLanguageConfigDTO.builder().languages(langs).build();
                    configMap.put(TenantConfigKeyEnum.SUPPORTED_LANGUAGES,
                            objectMapper.writeValueAsString(languageConfig));
                }
            }

            // Add LGD Location Hierarchy
            if (effectiveKeys.contains(TenantConfigKeyEnum.LGD_LOCATION_HIERARCHY)) {
                TenantLocationHierarchyConfigDTO lgdHierarchy = tenantSchemaRepository.getLocationHierarchy(schemaName,
                        RegionTypeEnum.LGD);
                if (lgdHierarchy != null && lgdHierarchy.getLocationHierarchy() != null
                        && lgdHierarchy.getLocationHierarchy().size() > 0) {
                    configMap.put(TenantConfigKeyEnum.LGD_LOCATION_HIERARCHY,
                            objectMapper.writeValueAsString(lgdHierarchy));
                }
            }

            // Add Dept Location Hierarchy
            if (effectiveKeys.contains(TenantConfigKeyEnum.DEPT_LOCATION_HIERARCHY)) {
                TenantLocationHierarchyConfigDTO deptHierarchy = tenantSchemaRepository.getLocationHierarchy(schemaName,
                        RegionTypeEnum.DEPARTMENT);
                if (deptHierarchy != null && deptHierarchy.getLocationHierarchy() != null
                        && deptHierarchy.getLocationHierarchy().size() > 0) {
                    configMap.put(TenantConfigKeyEnum.DEPT_LOCATION_HIERARCHY,
                            objectMapper.writeValueAsString(deptHierarchy));
                }
            }

            return TenantConfigResponseDTO.builder()
                    .tenantId(tenantId)
                    .configs(configMap)
                    .build();
        } catch (Exception e) {
            log.error("Failed to fetch configurations for tenant [id={}]", tenantId, e);
            throw new RuntimeException("Failed to retrieve tenant configurations: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public TenantConfigResponseDTO setTenantConfigs(Integer tenantId, SetTenantConfigRequestDTO request) {
        log.info("Setting tenant configurations [id={}]", tenantId);
        TenantResponseDTO tenant = tenantCommonRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Tenant with tenantId " + tenantId + " does not exist"));

        try {
            String currentUuid = SecurityUtils.getCurrentUserUuid();
            Integer currentUserId = tenantCommonRepository.findUserIdByUuid(currentUuid).orElse(null);

            Map<TenantConfigKeyEnum, String> results = new HashMap<>();

            for (Map.Entry<TenantConfigKeyEnum, String> entry : request.getConfigs().entrySet()) {
                TenantConfigKeyEnum key = entry.getKey();
                String value = entry.getValue();

                if (key.getType() == ConfigType.GENERIC) {
                    InternalConfigDTO cfg = tenantCommonRepository
                            .upsertConfig(tenantId, key.name(), value, currentUserId)
                            .orElseThrow(() -> new RuntimeException("Failed to upsert configuration for key: " + key));
                    results.put(TenantConfigKeyEnum.valueOf(cfg.getConfigKey()), cfg.getConfigValue());
                } else {
                    // Specialized handling
                    String schemaName = "tenant_" + tenant.getStateCode().toLowerCase();
                    handleSpecializedConfig(schemaName, key, value, currentUserId);
                    results.put(key, value);
                }
            }

            return TenantConfigResponseDTO.builder()
                    .tenantId(tenantId)
                    .configs(results)
                    .build();
        } catch (Exception e) {
            log.error("Failed to set configurations for tenant [id={}]", tenantId, e);
            throw new RuntimeException("Tenant configurations upsert failed: " + e.getMessage(), e);
        }
    }

    private void handleSpecializedConfig(String schemaName, TenantConfigKeyEnum key, String value,
            Integer currentUserId) throws Exception {
        switch (key) {
            case SUPPORTED_LANGUAGES:
                TenantLanguageConfigDTO langReq = objectMapper.readValue(value, TenantLanguageConfigDTO.class);
                tenantSchemaRepository.setSupportedLanguages(schemaName, langReq.getLanguages(), currentUserId);
                break;
            case LGD_LOCATION_HIERARCHY:
                TenantLocationHierarchyConfigDTO lgdReq = objectMapper.readValue(value,
                        TenantLocationHierarchyConfigDTO.class);
                tenantSchemaRepository.setLocationHierarchy(schemaName, RegionTypeEnum.LGD,
                        lgdReq.getLocationHierarchy(), currentUserId);
                break;
            case DEPT_LOCATION_HIERARCHY:
                TenantLocationHierarchyConfigDTO deptReq = objectMapper.readValue(value,
                        TenantLocationHierarchyConfigDTO.class);
                tenantSchemaRepository.setLocationHierarchy(schemaName, RegionTypeEnum.DEPARTMENT,
                        deptReq.getLocationHierarchy(), currentUserId);
                break;
            default:
                throw new UnsupportedOperationException("No specialized handler for: " + key);
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

    private void cacheTenantInRedis(TenantResponseDTO tenant, String schemaName) {
        String tenantStateCode = tenant.getStateCode().toUpperCase();
        String tenantKey = "tenant-service:tenants:" + tenantStateCode + ":profile";

        Map<String, String> tenantPayload = new HashMap<>();
        tenantPayload.put("id", String.valueOf(tenant.getId()));
        tenantPayload.put("stateCode", tenantStateCode);
        tenantPayload.put("name", tenant.getName());
        tenantPayload.put("status", tenant.getStatus());
        tenantPayload.put("schemaName", schemaName);

        redisTemplate.opsForHash().putAll(tenantKey, tenantPayload);
        redisTemplate.opsForSet().add("tenant-service:tenants:index", tenantStateCode);
        log.info("Tenant cached in Redis under key: {}", tenantKey);
    }
}
