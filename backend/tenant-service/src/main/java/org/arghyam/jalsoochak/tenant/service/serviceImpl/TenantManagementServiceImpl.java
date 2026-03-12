package org.arghyam.jalsoochak.tenant.service.serviceImpl;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.arghyam.jalsoochak.tenant.config.TenantContext;
import org.arghyam.jalsoochak.tenant.config.TenantDefaultsProperties;
import org.arghyam.jalsoochak.tenant.event.TenantCreatedEvent;
import org.arghyam.jalsoochak.tenant.event.TenantDeactivatedEvent;
import org.arghyam.jalsoochak.tenant.event.TenantUpdatedEvent;
import org.arghyam.jalsoochak.tenant.dto.common.PageResponseDTO;
import org.arghyam.jalsoochak.tenant.dto.request.CreateDepartmentRequestDTO;
import org.arghyam.jalsoochak.tenant.dto.request.CreateTenantRequestDTO;
import org.arghyam.jalsoochak.tenant.dto.request.SetTenantConfigRequestDTO;
import org.arghyam.jalsoochak.tenant.dto.request.UpdateTenantRequestDTO;
import org.arghyam.jalsoochak.tenant.dto.response.DepartmentResponseDTO;
import org.arghyam.jalsoochak.tenant.dto.response.TenantConfigResponseDTO;
import org.arghyam.jalsoochak.tenant.dto.response.TenantResponseDTO;
import org.arghyam.jalsoochak.tenant.dto.response.LocationResponseDTO;
import org.arghyam.jalsoochak.tenant.dto.response.LocationHierarchyResponseDTO;
import org.arghyam.jalsoochak.tenant.dto.internal.ConfigDTO;
import org.arghyam.jalsoochak.tenant.dto.internal.ConfigValueDTO;
import org.arghyam.jalsoochak.tenant.dto.internal.LanguageConfigDTO;
import org.arghyam.jalsoochak.tenant.dto.internal.LanguageListConfigDTO;
import org.arghyam.jalsoochak.tenant.dto.internal.LocationConfigDTO;
import org.arghyam.jalsoochak.tenant.dto.internal.LocationLevelConfigDTO;
import org.arghyam.jalsoochak.tenant.dto.internal.ReasonListConfigDTO;
import org.arghyam.jalsoochak.tenant.dto.internal.SimpleConfigValueDTO;
import org.arghyam.jalsoochak.tenant.enums.RegionTypeEnum;
import org.arghyam.jalsoochak.tenant.enums.TenantConfigKeyEnum;
import org.arghyam.jalsoochak.tenant.enums.TenantConfigKeyEnum.ConfigType;
import org.arghyam.jalsoochak.tenant.exception.ConfigurationException;
import org.arghyam.jalsoochak.tenant.exception.InvalidConfigKeyException;
import org.arghyam.jalsoochak.tenant.exception.InvalidConfigValueException;
import org.arghyam.jalsoochak.tenant.exception.ResourceNotFoundException;
import org.arghyam.jalsoochak.tenant.repository.TenantCommonRepository;
import org.arghyam.jalsoochak.tenant.repository.TenantSchemaRepository;
import org.arghyam.jalsoochak.tenant.service.TenantManagementService;
import org.arghyam.jalsoochak.tenant.service.TenantSchedulerManager;
import org.arghyam.jalsoochak.tenant.util.SecurityUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

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
    private final ObjectMapper objectMapper;
    private final TenantDefaultsProperties tenantDefaults;
    private final ApplicationEventPublisher eventPublisher;
    private final TenantSchedulerManager schedulerManager;

    @Override
    @Transactional
    public TenantResponseDTO createTenant(CreateTenantRequestDTO request) {
        log.info("Creating tenant – stateCode: {}, name: {}", request.getStateCode(), request.getName());

        tenantCommonRepository.findByStateCode(request.getStateCode()).ifPresent(existing -> {
            throw new IllegalStateException(
                    "Tenant with state code '" + request.getStateCode() + "' already exists");
        });

        Integer currentUserId = resolveCurrentUserId();

        TenantResponseDTO tenant = tenantCommonRepository.createTenant(request, currentUserId)
                .orElseThrow(() -> new RuntimeException("Tenant creation failed – no record returned"));
        log.info("Tenant record created in common_schema with id: {}", tenant.getId());

        String schemaName = "tenant_" + request.getStateCode().toLowerCase();
        tenantCommonRepository.provisionTenantSchema(schemaName);
        log.info("Tenant schema '{}' provisioned successfully", schemaName);

        setDefaultConfigs(tenant, schemaName, currentUserId);
        eventPublisher.publishEvent(new TenantCreatedEvent(tenant, schemaName));

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

        Integer currentUserId = resolveCurrentUserId();

        TenantResponseDTO updated = tenantCommonRepository.updateTenant(tenantId, request, currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Tenant with tenantId " + tenantId + " does not exist"));
        log.info("Tenant [id={}] updated successfully", tenantId);
        eventPublisher.publishEvent(new TenantUpdatedEvent(updated));
        return updated;
    }

    @Override
    @Transactional
    public void deactivateTenant(Integer tenantId) {
        log.info("Deactivating tenant [id={}]", tenantId);

        tenantCommonRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Tenant with tenantId " + tenantId + " does not exist"));

        Integer currentUserId = resolveCurrentUserId();

        tenantCommonRepository.deactivateTenant(tenantId, currentUserId);
        log.info("Tenant [id={}] deactivated successfully", tenantId);

        tenantCommonRepository.findById(tenantId)
                .ifPresent(tenant -> eventPublisher.publishEvent(new TenantDeactivatedEvent(tenant)));
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
        Integer currentUserId = resolveCurrentUserId();
        return tenantSchemaRepository.createDepartment(schemaName, request, currentUserId)
                .orElseThrow(() -> new RuntimeException("Department creation failed – no record returned"));
    }

    @Override
    public PageResponseDTO<TenantResponseDTO> getAllTenants(int page, int size) {
        long offset = (long) page * size;
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
            LocationConfigDTO lgdHierarchy = tenantSchemaRepository.getLocationHierarchy(schemaName, RegionTypeEnum.LGD);
            List<LocationLevelConfigDTO> levels = lgdHierarchy != null ? lgdHierarchy.getLocationHierarchy() : null;
            if (levels != null && !levels.isEmpty()) {
                configMap.put(TenantConfigKeyEnum.LGD_LOCATION_HIERARCHY,
                        LocationConfigDTO.builder().locationHierarchy(levels).build());
            }
        }

        if (effectiveKeys.contains(TenantConfigKeyEnum.DEPT_LOCATION_HIERARCHY)) {
            LocationConfigDTO deptHierarchy = tenantSchemaRepository.getLocationHierarchy(schemaName, RegionTypeEnum.DEPARTMENT);
            List<LocationLevelConfigDTO> levels = deptHierarchy != null ? deptHierarchy.getLocationHierarchy() : null;
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

        Integer currentUserId = resolveCurrentUserId();

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

        // Only reschedule when a schedule-bearing key was actually updated, and defer
        // the call to after the transaction commits so a bad schedule config cannot
        // roll back an otherwise-valid config write (e.g. SUPPORTED_LANGUAGES).
        Set<TenantConfigKeyEnum> scheduleKeys = EnumSet.of(
                TenantConfigKeyEnum.PUMP_OPERATOR_REMINDER_NUDGE_TIME,
                TenantConfigKeyEnum.FIELD_STAFF_ESCALATION_RULES);
        boolean hasScheduleKey = request.getConfigs().keySet().stream()
                .anyMatch(scheduleKeys::contains);
        if (hasScheduleKey) {
            final int finalTenantId = tenantId;
            final String finalStateCode = tenant.getStateCode();
            if (TransactionSynchronizationManager.isSynchronizationActive()) {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        schedulerManager.rescheduleForTenant(finalTenantId, finalStateCode);
                    }
                });
            } else {
                schedulerManager.rescheduleForTenant(finalTenantId, finalStateCode);
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


    @Override
    public LocationHierarchyResponseDTO getLocationHierarchy(Integer tenantId, String hierarchyType) {
        log.info("Fetching location hierarchy [id={}, hierarchyType={}]", tenantId, hierarchyType);
        
        TenantResponseDTO tenant = tenantCommonRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Tenant with tenantId " + tenantId + " does not exist"));

        String schemaName = "tenant_" + tenant.getStateCode().toLowerCase();

        try {
            RegionTypeEnum regionType = RegionTypeEnum.valueOf(hierarchyType.toUpperCase());
            LocationConfigDTO hierarchyConfig = tenantSchemaRepository.getLocationHierarchy(schemaName, regionType);
            
            if (hierarchyConfig == null || hierarchyConfig.getLocationHierarchy() == null) {
                throw new ResourceNotFoundException(
                        "Hierarchy configuration not found for type: " + hierarchyType + " in tenant [id=" + tenantId + "]");
            }

            log.info("Location hierarchy retrieved successfully [id={}, hierarchyType={}]", tenantId, hierarchyType);
            
            return LocationHierarchyResponseDTO.builder()
                    .hierarchyType(hierarchyType)
                    .levels(hierarchyConfig.getLocationHierarchy())
                    .build();
        } catch (IllegalArgumentException e) {
            log.error("Invalid hierarchy type [id={}, hierarchyType={}]", tenantId, hierarchyType, e);
            throw new IllegalArgumentException("Invalid hierarchy type: " + hierarchyType + ". Valid values: LGD, DEPARTMENT", e);
        }
    }

    @Override
    public List<LocationResponseDTO> getLocationChildren(Integer tenantId, String hierarchyType, Integer parentId) {
        log.info("Fetching location children [id={}, hierarchyType={}, parentId={}]", tenantId, hierarchyType, parentId);
        
        TenantResponseDTO tenant = tenantCommonRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Tenant with tenantId " + tenantId + " does not exist"));

        String schemaName = "tenant_" + tenant.getStateCode().toLowerCase();

        try {
            RegionTypeEnum regionType = RegionTypeEnum.valueOf(hierarchyType.toUpperCase());
            
            List<LocationResponseDTO> children;
            if (RegionTypeEnum.LGD.equals(regionType)) {
                children = tenantSchemaRepository.findLgdLocationsByParentId(schemaName, parentId);
            } else if (RegionTypeEnum.DEPARTMENT.equals(regionType)) {
                children = tenantSchemaRepository.findDepartmentLocationsByParentId(schemaName, parentId);
            } else {
                throw new IllegalArgumentException("Unknown hierarchy type: " + hierarchyType);
            }

            log.info("Location children retrieved successfully [id={}, hierarchyType={}, count={}]", 
                    tenantId, hierarchyType, children.size());
            
            return children;
        } catch (IllegalArgumentException e) {
            log.error("Invalid hierarchy type [id={}, hierarchyType={}]", tenantId, hierarchyType, e);
            throw new IllegalArgumentException("Invalid hierarchy type: " + hierarchyType + ". Valid values: LGD, DEPARTMENT", e);
        }
    }


    private Integer resolveCurrentUserId() {
        String uuid = SecurityUtils.getCurrentUserUuid();
        // TODO: uncomment below to enforce non-null actor once ResourceNotFoundException is wired for auth context
        // return tenantCommonRepository.findUserIdByUuid(uuid)
        //         .orElseThrow(() -> new ResourceNotFoundException("Current user not found for uuid: " + uuid));
        return tenantCommonRepository.findUserIdByUuid(uuid).orElse(null);
    }

    private void setDefaultConfigs(TenantResponseDTO tenant, String schemaName, Integer currentUserId) {
        tenantSchemaRepository.setLocationHierarchy(
                schemaName, RegionTypeEnum.LGD, tenantDefaults.getLgdLocationHierarchy(), currentUserId);

        tenantSchemaRepository.setLocationHierarchy(
                schemaName, RegionTypeEnum.DEPARTMENT, tenantDefaults.getDeptLocationHierarchy(), currentUserId);

        try {
            ReasonListConfigDTO reasons = ReasonListConfigDTO.builder()
                    .reasons(tenantDefaults.getMeterChangeReasons())
                    .build();
            tenantCommonRepository.upsertConfig(tenant.getId(),
                    TenantConfigKeyEnum.METER_CHANGE_REASONS.name(),
                    objectMapper.writeValueAsString(reasons), currentUserId)
                    .orElseThrow(() -> new ConfigurationException(
                            "Failed to seed METER_CHANGE_REASONS for tenant [id=" + tenant.getId() + ", userId=" + currentUserId + "]"));

            tenantCommonRepository.upsertConfig(tenant.getId(),
                    TenantConfigKeyEnum.LOCATION_CHECK_REQUIRED.name(),
                    objectMapper.writeValueAsString(new SimpleConfigValueDTO("NO")), currentUserId)
                    .orElseThrow(() -> new ConfigurationException(
                            "Failed to seed LOCATION_CHECK_REQUIRED for tenant [id=" + tenant.getId() + ", userId=" + currentUserId + "]"));
        } catch (JsonProcessingException e) {
            throw new InvalidConfigValueException("Failed to serialize default tenant configs", e);
        }

        log.info("Default configs seeded for tenant [id={}]", tenant.getId());
    }
}
