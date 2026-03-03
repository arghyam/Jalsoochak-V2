package org.arghyam.jalsoochak.tenant.service.serviceImpl;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.arghyam.jalsoochak.tenant.config.TenantContext;
import org.arghyam.jalsoochak.tenant.dto.common.PageResponseDTO;
import org.arghyam.jalsoochak.tenant.dto.request.CreateDepartmentRequestDTO;
import org.arghyam.jalsoochak.tenant.dto.request.CreateTenantRequestDTO;
import org.arghyam.jalsoochak.tenant.dto.request.SetTenantConfigRequestDTO;
import org.arghyam.jalsoochak.tenant.dto.request.UpdateTenantRequestDTO;
import org.arghyam.jalsoochak.tenant.dto.response.DepartmentResponseDTO;
import org.arghyam.jalsoochak.tenant.dto.response.TenantConfigResponseDTO;
import org.arghyam.jalsoochak.tenant.dto.response.TenantResponseDTO;
import org.arghyam.jalsoochak.tenant.dto.internal.ConfigDTO;
import org.arghyam.jalsoochak.tenant.dto.internal.ConfigValueDTO;
import org.arghyam.jalsoochak.tenant.dto.internal.LanguageConfigDTO;
import org.arghyam.jalsoochak.tenant.dto.internal.LanguageListConfigDTO;
import org.arghyam.jalsoochak.tenant.dto.internal.LocationConfigDTO;
import org.arghyam.jalsoochak.tenant.dto.internal.LocationLevelConfigDTO;
import org.arghyam.jalsoochak.tenant.enums.RegionTypeEnum;
import org.arghyam.jalsoochak.tenant.enums.TenantConfigKeyEnum;
import org.arghyam.jalsoochak.tenant.enums.TenantConfigKeyEnum.ConfigType;
import org.arghyam.jalsoochak.tenant.exception.InvalidConfigKeyException;
import org.arghyam.jalsoochak.tenant.exception.InvalidConfigValueException;
import org.arghyam.jalsoochak.tenant.exception.ResourceNotFoundException;
import org.arghyam.jalsoochak.tenant.kafka.KafkaProducer;
import org.arghyam.jalsoochak.tenant.repository.TenantCommonRepository;
import org.arghyam.jalsoochak.tenant.repository.TenantSchemaRepository;
import org.arghyam.jalsoochak.tenant.service.TenantManagementService;
import org.arghyam.jalsoochak.tenant.util.SecurityUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
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

        String uuid = SecurityUtils.getCurrentUserUuid();
        Integer currentUserId = tenantCommonRepository.findUserIdByUuid(uuid).orElse(null);

        TenantResponseDTO updated = tenantCommonRepository.updateTenant(tenantId, request, currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Tenant with tenantId " + tenantId + " does not exist"));
        log.info("Tenant [id={}] updated successfully", tenantId);
        publishTenantEvent(updated, "TENANT_UPDATED");
        return updated;
    }

    @Override
    @Transactional
    public void deactivateTenant(Integer tenantId) {
        log.info("Deactivating tenant [id={}]", tenantId);

        tenantCommonRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Tenant with tenantId " + tenantId + " does not exist"));

        String uuid = SecurityUtils.getCurrentUserUuid();
        Integer currentUserId = tenantCommonRepository.findUserIdByUuid(uuid).orElse(null);

        tenantCommonRepository.deactivateTenant(tenantId, currentUserId);
        log.info("Tenant [id={}] deactivated successfully", tenantId);

        tenantCommonRepository.findById(tenantId)
                .ifPresent(tenant -> publishTenantEvent(tenant, "TENANT_DEACTIVATED"));
    }

    @Override
    public List<DepartmentResponseDTO> getTenantDepartments() {
        String schemaName = TenantContext.getSchema();
        if (schemaName == null || schemaName.isBlank()) {
            throw new IllegalArgumentException(
                    "Tenant could not be resolved. Ensure the X-Tenant-Code header is set by the gateway.");
        }
        log.info("Fetching departments from schema: {}", schemaName);
        return tenantSchemaRepository.getDepartments(schemaName);
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
        String currentUuid = SecurityUtils.getCurrentUserUuid();
        Integer currentUserId = tenantCommonRepository.findUserIdByUuid(currentUuid).orElse(null);
        return tenantSchemaRepository.createDepartment(schemaName, request, currentUserId)
                .orElseThrow(() -> new RuntimeException("Department creation failed – no record returned"));
    }

    @Override
    public PageResponseDTO<TenantResponseDTO> getAllTenants(int page, int size) {
        int offset = page * size;
        List<TenantResponseDTO> tenants = tenantCommonRepository.findAll(size, offset);
        long totalElements = tenantCommonRepository.countAllTenants();
        return PageResponseDTO.of(tenants, totalElements, page, size);
    }

    @Override
    public TenantConfigResponseDTO getTenantConfigs(Integer tenantId, Set<TenantConfigKeyEnum> keys) {
        log.info("Fetching tenant configurations [id={}, keys={}]", tenantId, keys);
        TenantResponseDTO tenant = tenantCommonRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Tenant with tenantId " + tenantId + " does not exist"));

        Set<TenantConfigKeyEnum> effectiveKeys = (keys == null || keys.isEmpty())
                ? EnumSet.allOf(TenantConfigKeyEnum.class)
                : keys;

        Map<TenantConfigKeyEnum, ConfigValueDTO> configMap = new HashMap<>();

        List<ConfigDTO> configs = tenantCommonRepository.findConfigsByTenantId(tenantId);
        for (ConfigDTO cfg : configs) {
            try {
                TenantConfigKeyEnum key = TenantConfigKeyEnum.valueOf(cfg.getConfigKey());
                if (effectiveKeys.contains(key)) {
                    configMap.put(key, objectMapper.readValue(cfg.getConfigValue(), key.getDtoClass()));
                }
            } catch (IllegalArgumentException e) {
                log.error("Invalid tenant config key [key={}]", cfg.getConfigKey(), e);
                throw new InvalidConfigKeyException("Invalid tenant config key: " + cfg.getConfigKey(), e);
            } catch (JsonProcessingException e) {
                log.error("Malformed config value for key [key={}]", cfg.getConfigKey(), e);
                throw new InvalidConfigValueException("Malformed config value for key: " + cfg.getConfigKey(), e);
            }
        }

        String schemaName = "tenant_" + tenant.getStateCode().toLowerCase();

        if (effectiveKeys.contains(TenantConfigKeyEnum.SUPPORTED_LANGUAGES)) {
            List<LanguageConfigDTO> langs = tenantSchemaRepository.getSupportedLanguages(schemaName);
            if (langs != null && !langs.isEmpty()) {
                configMap.put(TenantConfigKeyEnum.SUPPORTED_LANGUAGES,
                        LanguageListConfigDTO.builder().languages(langs).build());
            }
        }

        if (effectiveKeys.contains(TenantConfigKeyEnum.LGD_LOCATION_HIERARCHY)) {
            List<LocationLevelConfigDTO> levels = tenantSchemaRepository
                    .getLocationHierarchy(schemaName, RegionTypeEnum.LGD).getLocationHierarchy();
            if (levels != null && !levels.isEmpty()) {
                configMap.put(TenantConfigKeyEnum.LGD_LOCATION_HIERARCHY,
                        LocationConfigDTO.builder().locationHierarchy(levels).build());
            }
        }

        if (effectiveKeys.contains(TenantConfigKeyEnum.DEPT_LOCATION_HIERARCHY)) {
            List<LocationLevelConfigDTO> levels = tenantSchemaRepository
                    .getLocationHierarchy(schemaName, RegionTypeEnum.DEPARTMENT).getLocationHierarchy();
            if (levels != null && !levels.isEmpty()) {
                configMap.put(TenantConfigKeyEnum.DEPT_LOCATION_HIERARCHY,
                        LocationConfigDTO.builder().locationHierarchy(levels).build());
            }
        }

        return TenantConfigResponseDTO.builder()
                .tenantId(tenantId)
                .configs(configMap)
                .build();
    }

    @Override
    @Transactional
    public TenantConfigResponseDTO setTenantConfigs(Integer tenantId, SetTenantConfigRequestDTO request) {
        log.info("Setting tenant configurations [id={}]", tenantId);
        TenantResponseDTO tenant = tenantCommonRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Tenant with tenantId " + tenantId + " does not exist"));

        String currentUuid = SecurityUtils.getCurrentUserUuid();
        Integer currentUserId = tenantCommonRepository.findUserIdByUuid(currentUuid).orElse(null);

        Map<TenantConfigKeyEnum, ConfigValueDTO> results = new HashMap<>();

        for (Map.Entry<TenantConfigKeyEnum, JsonNode> entry : request.getConfigs().entrySet()) {
            TenantConfigKeyEnum key = entry.getKey();
            ConfigValueDTO dto;
            try {
                dto = objectMapper.treeToValue(entry.getValue(), key.getDtoClass());
            } catch (JsonProcessingException e) {
                throw new InvalidConfigValueException(
                        "Invalid value for config key " + key + ": " + e.getMessage(), e);
            }

            if (key.getType() == ConfigType.GENERIC) {
                String serialized;
                try {
                    serialized = objectMapper.writeValueAsString(dto);
                } catch (JsonProcessingException e) {
                    throw new InvalidConfigValueException(
                            "Failed to serialize config value for key: " + key, e);
                }
                ConfigDTO cfg = tenantCommonRepository
                        .upsertConfig(tenantId, key.name(), serialized, currentUserId)
                        .orElseThrow(() -> new RuntimeException(
                                "Failed to upsert configuration for key: " + key));
                try {
                    results.put(TenantConfigKeyEnum.valueOf(cfg.getConfigKey()),
                            objectMapper.readValue(cfg.getConfigValue(), key.getDtoClass()));
                } catch (JsonProcessingException e) {
                    throw new InvalidConfigValueException(
                            "Malformed saved config value for key: " + key, e);
                }
            } else {
                String schemaName = "tenant_" + tenant.getStateCode().toLowerCase();
                handleSpecializedConfig(schemaName, key, dto, currentUserId);
                results.put(key, dto);
            }
        }

        return TenantConfigResponseDTO.builder()
                .tenantId(tenantId)
                .configs(results)
                .build();
    }

    private void handleSpecializedConfig(String schemaName, TenantConfigKeyEnum key, ConfigValueDTO dto,
            Integer currentUserId) {
        switch (key) {
            case SUPPORTED_LANGUAGES -> {
                if (!(dto instanceof LanguageListConfigDTO langDto)) {
                    throw new InvalidConfigValueException(
                            "Expected LanguageListConfigDTO for SUPPORTED_LANGUAGES, got "
                                    + dto.getClass().getSimpleName());
                }
                tenantSchemaRepository.setSupportedLanguages(schemaName, langDto.getLanguages(), currentUserId);
            }
            case LGD_LOCATION_HIERARCHY -> {
                if (!(dto instanceof LocationConfigDTO locDto)) {
                    throw new InvalidConfigValueException(
                            "Expected LocationConfigDTO for LGD_LOCATION_HIERARCHY, got "
                                    + dto.getClass().getSimpleName());
                }
                tenantSchemaRepository.setLocationHierarchy(schemaName, RegionTypeEnum.LGD,
                        locDto.getLocationHierarchy(), currentUserId);
            }
            case DEPT_LOCATION_HIERARCHY -> {
                if (!(dto instanceof LocationConfigDTO locDto)) {
                    throw new InvalidConfigValueException(
                            "Expected LocationConfigDTO for DEPT_LOCATION_HIERARCHY, got "
                                    + dto.getClass().getSimpleName());
                }
                tenantSchemaRepository.setLocationHierarchy(schemaName, RegionTypeEnum.DEPARTMENT,
                        locDto.getLocationHierarchy(), currentUserId);
            }
            default -> throw new UnsupportedOperationException("No specialized handler for: " + key);
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
            kafkaProducer.sendMessage(objectMapper.writeValueAsString(event));
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
