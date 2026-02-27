package org.arghyam.jalsoochak.tenant.service.serviceImpl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.arghyam.jalsoochak.tenant.dto.internal.ConfigDTO;
import org.arghyam.jalsoochak.tenant.dto.request.SetSystemConfigRequestDTO;
import org.arghyam.jalsoochak.tenant.dto.response.SystemConfigResponseDTO;
import org.arghyam.jalsoochak.tenant.enums.SystemConfigKeyEnum;
import org.arghyam.jalsoochak.tenant.repository.TenantCommonRepository;
import org.arghyam.jalsoochak.tenant.service.SystemManagementService;
import org.arghyam.jalsoochak.tenant.util.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class SystemManagementServiceImpl implements SystemManagementService {

    private static final Integer SYSTEM_TENANT_ID = 0;
    private final TenantCommonRepository tenantCommonRepository;

    @Override
    public SystemConfigResponseDTO getSystemConfigs(Set<SystemConfigKeyEnum> keys) {
        log.info("Fetching system configurations [keys={}]", keys);
        Set<SystemConfigKeyEnum> effectiveKeys = (keys == null || keys.isEmpty())
                ? EnumSet.allOf(SystemConfigKeyEnum.class)
                : keys;

        try {
            List<ConfigDTO> configs = tenantCommonRepository.findConfigsByTenantId(SYSTEM_TENANT_ID);
            Map<SystemConfigKeyEnum, String> configMap = new HashMap<>();

            for (ConfigDTO cfg : configs) {
                try {
                    SystemConfigKeyEnum key = SystemConfigKeyEnum.valueOf(cfg.getConfigKey());
                    if (effectiveKeys.contains(key)) {
                        configMap.put(key, cfg.getConfigValue());
                    }
                } catch (IllegalArgumentException e) {
                    log.error("Invalid system config key: {}", cfg.getConfigKey(), e);
                }
            }

            return SystemConfigResponseDTO.builder().configs(configMap).build();
        } catch (Exception e) {
            log.error("Failed to retrieve system configurations", e);
            throw new RuntimeException("Failed to retrieve system configurations" + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public SystemConfigResponseDTO setSystemConfigs(SetSystemConfigRequestDTO request) {
        log.info("Setting system configurations");
        try {
            String currentUuid = SecurityUtils.getCurrentUserUuid();
            Integer currentUserId = tenantCommonRepository.findUserIdByUuid(currentUuid).orElse(null);

            Map<SystemConfigKeyEnum, String> results = new HashMap<>();
            for (Map.Entry<SystemConfigKeyEnum, String> entry : request.getConfigs().entrySet()) {
                SystemConfigKeyEnum key = entry.getKey();
                String value = entry.getValue();

                ConfigDTO cfg = tenantCommonRepository
                        .upsertConfig(SYSTEM_TENANT_ID, key.name(), value, currentUserId)
                        .orElseThrow(() -> new RuntimeException("Failed to upsert system config: " + key));
                results.put(SystemConfigKeyEnum.valueOf(cfg.getConfigKey()), cfg.getConfigValue());
            }

            return SystemConfigResponseDTO.builder().configs(results).build();
        } catch (Exception e) {
            log.error("Failed to update system configurations", e);
            throw new RuntimeException("Failed to update system configurations" + e.getMessage(), e);
        }
    }
}
