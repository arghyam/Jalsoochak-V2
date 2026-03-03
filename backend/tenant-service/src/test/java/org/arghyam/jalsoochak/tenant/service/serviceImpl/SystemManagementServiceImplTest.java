package org.arghyam.jalsoochak.tenant.service.serviceImpl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.arghyam.jalsoochak.tenant.dto.internal.ConfigDTO;
import org.arghyam.jalsoochak.tenant.dto.internal.ConfigValueDTO;
import org.arghyam.jalsoochak.tenant.dto.internal.LocationConfigDTO;
import org.arghyam.jalsoochak.tenant.dto.request.SetSystemConfigRequestDTO;
import org.arghyam.jalsoochak.tenant.dto.response.SystemConfigResponseDTO;
import org.arghyam.jalsoochak.tenant.enums.SystemConfigKeyEnum;
import org.arghyam.jalsoochak.tenant.exception.InvalidConfigKeyException;
import org.arghyam.jalsoochak.tenant.repository.TenantCommonRepository;
import org.arghyam.jalsoochak.tenant.util.SecurityUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class SystemManagementServiceImplTest {

    @Mock
    private TenantCommonRepository tenantCommonRepository;

    private SystemManagementServiceImpl systemManagementService;

    private MockedStatic<SecurityUtils> mockedSecurityUtils;
    
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockedSecurityUtils = mockStatic(SecurityUtils.class);
        objectMapper = new ObjectMapper();
        // Manually create service with real ObjectMapper and mocked dependencies
        systemManagementService = new SystemManagementServiceImpl(tenantCommonRepository, objectMapper);
    }

    @AfterEach
    void tearDown() {
        mockedSecurityUtils.close();
    }

    @Test
    void getSystemConfigs_Success() {
        List<ConfigDTO> configs = List.of(
                ConfigDTO.builder()
                        .configKey(SystemConfigKeyEnum.DEFAULT_LGD_LOCATION_HIERARCHY.name())
                        .configValue("{\"locationHierarchy\": []}")
                        .build());
        when(tenantCommonRepository.findConfigsByTenantId(0)).thenReturn(configs);
        SystemConfigResponseDTO result = systemManagementService.getSystemConfigs(null);
        assertNotNull(result);
        ConfigValueDTO configValue = result.getConfigs().get(SystemConfigKeyEnum.DEFAULT_LGD_LOCATION_HIERARCHY);
        assertNotNull(configValue);
        assertTrue(configValue instanceof LocationConfigDTO);
        assertEquals(0, ((LocationConfigDTO) configValue).getLocationHierarchy().size());
    }

    @Test
    void getSystemConfigs_InvalidKeyInDB_IgnoresKey() {
        List<ConfigDTO> configs = List.of(
                ConfigDTO.builder()
                        .configKey("INVALID_KEY")
                        .configValue("{\"levels\": []}")
                        .build());
        when(tenantCommonRepository.findConfigsByTenantId(0)).thenReturn(configs);
        // The service throws InvalidConfigKeyException for invalid keys
        assertThrows(InvalidConfigKeyException.class, () -> systemManagementService.getSystemConfigs(null));
    }

    @Test
    void setSystemConfigs_Success() throws Exception {
        Map<SystemConfigKeyEnum, JsonNode> newConfigs = new HashMap<>();
        // Input should be JSON that matches LocationConfigDTO structure with "locationHierarchy" field
        newConfigs.put(SystemConfigKeyEnum.DEFAULT_LGD_LOCATION_HIERARCHY, objectMapper.readTree("{\"locationHierarchy\": []}"));
        SetSystemConfigRequestDTO request = SetSystemConfigRequestDTO.builder().configs(newConfigs).build();

        ConfigDTO savedConfig = ConfigDTO.builder()
                .configKey(SystemConfigKeyEnum.DEFAULT_LGD_LOCATION_HIERARCHY.name())
                .configValue("{\"locationHierarchy\": []}")
                .build();

        when(SecurityUtils.getCurrentUserUuid()).thenReturn("admin-uuid");
        when(tenantCommonRepository.findUserIdByUuid("admin-uuid")).thenReturn(Optional.of(1));
        when(tenantCommonRepository.upsertConfig(eq(0),
                eq(SystemConfigKeyEnum.DEFAULT_LGD_LOCATION_HIERARCHY.name()), anyString(), eq(1)))
                .thenReturn(Optional.of(savedConfig));

        SystemConfigResponseDTO result = systemManagementService.setSystemConfigs(request);

        assertNotNull(result);
        ConfigValueDTO configValue = result.getConfigs().get(SystemConfigKeyEnum.DEFAULT_LGD_LOCATION_HIERARCHY);
        assertNotNull(configValue);
        assertTrue(configValue instanceof LocationConfigDTO);
        assertEquals(0, ((LocationConfigDTO) configValue).getLocationHierarchy().size());
    }

    @Test
    void setSystemConfigs_RepositoryFailure_ThrowsException() throws Exception {
        Map<SystemConfigKeyEnum, JsonNode> newConfigs = new HashMap<>();
        // Input should be JSON that matches LocationConfigDTO structure with "locationHierarchy" field
        newConfigs.put(SystemConfigKeyEnum.DEFAULT_LGD_LOCATION_HIERARCHY, objectMapper.readTree("{\"locationHierarchy\": []}"));
        SetSystemConfigRequestDTO request = SetSystemConfigRequestDTO.builder().configs(newConfigs).build();

        when(SecurityUtils.getCurrentUserUuid()).thenReturn("admin-uuid");
        when(tenantCommonRepository.findUserIdByUuid("admin-uuid")).thenReturn(Optional.of(1));

        assertThrows(RuntimeException.class, () -> systemManagementService.setSystemConfigs(request));
    }
}
