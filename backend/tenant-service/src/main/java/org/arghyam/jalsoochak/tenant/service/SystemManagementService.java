package org.arghyam.jalsoochak.tenant.service;

import org.arghyam.jalsoochak.tenant.dto.request.SetSystemConfigRequestDTO;
import org.arghyam.jalsoochak.tenant.dto.response.SystemConfigResponseDTO;
import org.arghyam.jalsoochak.tenant.enums.SystemConfigKeyEnum;

import java.util.Set;

/**
 * Service for platform-wide system configurations.
 */
public interface SystemManagementService {

    /**
     * Get platform-wide system configurations.
     * 
     * @param keys Optional filter for configuration keys.
     * @return System configurations response.
     */
    SystemConfigResponseDTO getSystemConfigs(Set<SystemConfigKeyEnum> keys);

    /**
     * Update platform-wide system configurations.
     * 
     * @param request Update request.
     * @return Updated system configurations.
     */
    SystemConfigResponseDTO setSystemConfigs(SetSystemConfigRequestDTO request);
}
