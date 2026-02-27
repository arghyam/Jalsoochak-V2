package org.arghyam.jalsoochak.tenant.service.serviceImpl;

import org.arghyam.jalsoochak.tenant.dto.InternalConfigDTO;
import org.arghyam.jalsoochak.tenant.dto.SetSystemConfigRequestDTO;
import org.arghyam.jalsoochak.tenant.dto.SystemConfigResponseDTO;
import org.arghyam.jalsoochak.tenant.enums.SystemConfigKeyEnum;
import org.arghyam.jalsoochak.tenant.repository.TenantCommonRepository;
import org.arghyam.jalsoochak.tenant.util.SecurityUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SystemManagementServiceImplTest {

    @Mock
    private TenantCommonRepository tenantCommonRepository;

    @InjectMocks
    private SystemManagementServiceImpl systemManagementService;

    private MockedStatic<SecurityUtils> mockedSecurityUtils;

    @BeforeEach
    void setUp() {
        mockedSecurityUtils = mockStatic(SecurityUtils.class);
    }

    @AfterEach
    void tearDown() {
        mockedSecurityUtils.close();
    }

    @Test
    void getSystemConfigs_Success() {
        List<InternalConfigDTO> configs = List.of(
                InternalConfigDTO.builder()
                        .configKey(SystemConfigKeyEnum.DEFAULT_LGD_LOCATION_HIERARCHY.name())
                        .configValue("{\"levels\": []}")
                        .build());
        when(tenantCommonRepository.findConfigsByTenantId(0)).thenReturn(configs);
        SystemConfigResponseDTO result = systemManagementService.getSystemConfigs(null);
        assertNotNull(result);
        assertEquals("{\"levels\": []}", result.getConfigs().get(SystemConfigKeyEnum.DEFAULT_LGD_LOCATION_HIERARCHY));
    }

    @Test
    void getSystemConfigs_InvalidKeyInDB_IgnoresKey() {
        List<InternalConfigDTO> configs = List.of(
                InternalConfigDTO.builder()
                        .configKey("INVALID_KEY")
                        .configValue("{\"levels\": []}")
                        .build());
        when(tenantCommonRepository.findConfigsByTenantId(0)).thenReturn(configs);
        SystemConfigResponseDTO result = systemManagementService.getSystemConfigs(null);
        assertNotNull(result);
        assertTrue(result.getConfigs().isEmpty());
    }

    @Test
    void setSystemConfigs_Success() {
        Map<SystemConfigKeyEnum, String> newConfigs = new HashMap<>();
        newConfigs.put(SystemConfigKeyEnum.DEFAULT_LGD_LOCATION_HIERARCHY, "{\"levels\": []}");
        SetSystemConfigRequestDTO request = SetSystemConfigRequestDTO.builder().configs(newConfigs).build();

        InternalConfigDTO savedConfig = InternalConfigDTO.builder()
                .configKey(SystemConfigKeyEnum.DEFAULT_LGD_LOCATION_HIERARCHY.name())
                .configValue("{\"levels\": []}")
                .build();

        when(SecurityUtils.getCurrentUserUuid()).thenReturn("admin-uuid");
        when(tenantCommonRepository.findUserIdByUuid("admin-uuid")).thenReturn(Optional.of(1));
        when(tenantCommonRepository.upsertConfig(eq(0),
                eq(SystemConfigKeyEnum.DEFAULT_LGD_LOCATION_HIERARCHY.name()), anyString(), eq(1)))
                .thenReturn(Optional.of(savedConfig));

        SystemConfigResponseDTO result = systemManagementService.setSystemConfigs(request);

        assertNotNull(result);
        assertEquals("{\"levels\": []}", result.getConfigs().get(SystemConfigKeyEnum.DEFAULT_LGD_LOCATION_HIERARCHY));
    }

    @Test
    void setSystemConfigs_RepositoryFailure_ThrowsException() {
        Map<SystemConfigKeyEnum, String> newConfigs = new HashMap<>();
        newConfigs.put(SystemConfigKeyEnum.DEFAULT_LGD_LOCATION_HIERARCHY, "{\"levels\": []}");
        SetSystemConfigRequestDTO request = SetSystemConfigRequestDTO.builder().configs(newConfigs).build();

        when(SecurityUtils.getCurrentUserUuid()).thenReturn("admin-uuid");
        when(tenantCommonRepository.findUserIdByUuid("admin-uuid")).thenReturn(Optional.of(1));
        when(tenantCommonRepository.upsertConfig(anyInt(), anyString(), anyString(), anyInt()))
                .thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> systemManagementService.setSystemConfigs(request));
    }
}
