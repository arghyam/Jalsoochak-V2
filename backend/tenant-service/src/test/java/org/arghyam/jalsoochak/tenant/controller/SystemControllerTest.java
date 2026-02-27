package org.arghyam.jalsoochak.tenant.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.arghyam.jalsoochak.tenant.dto.request.SetSystemConfigRequestDTO;
import org.arghyam.jalsoochak.tenant.dto.response.SystemConfigResponseDTO;
import org.arghyam.jalsoochak.tenant.enums.SystemConfigKeyEnum;
import org.arghyam.jalsoochak.tenant.service.SystemManagementService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SystemController.class)
@AutoConfigureMockMvc(addFilters = false)
class SystemControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SystemManagementService systemManagementService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void getSystemConfigs_Success() throws Exception {
        Map<SystemConfigKeyEnum, String> configs = new HashMap<>();
        configs.put(SystemConfigKeyEnum.DEFAULT_LGD_LOCATION_HIERARCHY, "{\"levels\": []}");
        SystemConfigResponseDTO response = SystemConfigResponseDTO.builder().configs(configs).build();

        when(systemManagementService.getSystemConfigs(any())).thenReturn(response);

        mockMvc.perform(get("/api/v1/system/config")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("System configurations retrieved successfully"))
                .andExpect(jsonPath("$.data.configs.DEFAULT_LGD_LOCATION_HIERARCHY").value("{\"levels\": []}"));
    }

    @Test
    void setSystemConfigs_Success() throws Exception {
        Map<SystemConfigKeyEnum, String> configs = new HashMap<>();
        configs.put(SystemConfigKeyEnum.DEFAULT_LGD_LOCATION_HIERARCHY, "{\"levels\": [\"State\"]}");
        SetSystemConfigRequestDTO request = SetSystemConfigRequestDTO.builder().configs(configs).build();
        SystemConfigResponseDTO response = SystemConfigResponseDTO.builder().configs(configs).build();

        when(systemManagementService.setSystemConfigs(any(SetSystemConfigRequestDTO.class))).thenReturn(response);

        mockMvc.perform(put("/api/v1/system/config")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("System configurations set successfully"))
                .andExpect(
                        jsonPath("$.data.configs.DEFAULT_LGD_LOCATION_HIERARCHY").value("{\"levels\": [\"State\"]}"));
    }
}
