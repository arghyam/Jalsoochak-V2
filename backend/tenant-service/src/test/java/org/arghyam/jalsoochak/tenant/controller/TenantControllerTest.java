package org.arghyam.jalsoochak.tenant.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.arghyam.jalsoochak.tenant.dto.common.PageResponseDTO;
import org.arghyam.jalsoochak.tenant.dto.request.CreateDepartmentRequestDTO;
import org.arghyam.jalsoochak.tenant.dto.request.CreateTenantRequestDTO;
import org.arghyam.jalsoochak.tenant.dto.request.SetTenantConfigRequestDTO;
import org.arghyam.jalsoochak.tenant.dto.request.UpdateTenantRequestDTO;
import org.arghyam.jalsoochak.tenant.dto.response.DepartmentResponseDTO;
import org.arghyam.jalsoochak.tenant.dto.response.TenantConfigResponseDTO;
import org.arghyam.jalsoochak.tenant.dto.response.TenantResponseDTO;
import org.arghyam.jalsoochak.tenant.enums.TenantConfigKeyEnum;
import org.arghyam.jalsoochak.tenant.exception.ResourceNotFoundException;
import org.arghyam.jalsoochak.tenant.service.TenantManagementService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.*;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TenantController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("Tenant Controller Tests")
class TenantControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TenantManagementService tenantManagementService;

    @Autowired
    private ObjectMapper objectMapper;

    @Nested
    @DisplayName("Create Tenant")
    class CreateTenantTests {
        @Test
        void createTenant_Success() throws Exception {
            CreateTenantRequestDTO request = CreateTenantRequestDTO.builder()
                    .name("Test Tenant")
                    .stateCode("KA")
                    .lgdCode(29)
                    .build();

            TenantResponseDTO response = TenantResponseDTO.builder().id(1).name("Test Tenant").stateCode("KA")
                    .status("ACTIVE").build();

            when(tenantManagementService.createTenant(any(CreateTenantRequestDTO.class))).thenReturn(response);

            mockMvc.perform(post("/api/v1/tenants")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.status").value(201))
                    .andExpect(jsonPath("$.message").value("Tenant created successfully"))
                    .andExpect(jsonPath("$.data.id").value(1))
                    .andExpect(jsonPath("$.data.name").value("Test Tenant"));

            verify(tenantManagementService, times(1)).createTenant(any());
        }

        @Test
        void createTenant_AlreadyExists() throws Exception {
            CreateTenantRequestDTO request = CreateTenantRequestDTO.builder()
                    .stateCode("KA")
                    .build();

            when(tenantManagementService.createTenant(any()))
                    .thenThrow(new IllegalStateException("Tenant already exists"));

            mockMvc.perform(post("/api/v1/tenants")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void createTenant_MissingStateCode() throws Exception {
            String requestJson = "{\"name\":\"Test\",\"lgdCode\":29}";

            mockMvc.perform(post("/api/v1/tenants")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestJson))
                    .andExpect(status().is4xxClientError());

            verify(tenantManagementService, never()).createTenant(any());
        }
    }

    @Nested
    @DisplayName("Get All Tenants")
    class GetAllTenantsTests {
        @Test
        void getAllTenants_Success() throws Exception {
            TenantResponseDTO tenant1 = TenantResponseDTO.builder().id(1).name("Test Tenant").stateCode("KA").build();
            TenantResponseDTO tenant2 = TenantResponseDTO.builder().id(2).name("Test Tenant 2").stateCode("KL").build();
            PageResponseDTO<TenantResponseDTO> pageResponse = PageResponseDTO.<TenantResponseDTO>builder()
                    .content(List.of(tenant1, tenant2))
                    .number(0)
                    .size(10)
                    .totalElements(2L)
                    .build();

            when(tenantManagementService.getAllTenants(anyInt(), anyInt())).thenReturn(pageResponse);

            mockMvc.perform(get("/api/v1/tenants")
                    .param("page", "0")
                    .param("size", "10")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value(200))
                    .andExpect(jsonPath("$.data.content", hasSize(2)))
                    .andExpect(jsonPath("$.data.totalElements").value(2));

            verify(tenantManagementService).getAllTenants(0, 10);
        }

        @Test
        void getAllTenants_EmptyList() throws Exception {
            PageResponseDTO<TenantResponseDTO> emptyPageResponse = PageResponseDTO.<TenantResponseDTO>builder()
                    .content(Collections.emptyList())
                    .number(0)
                    .size(10)
                    .totalElements(0L)
                    .build();

            when(tenantManagementService.getAllTenants(anyInt(), anyInt())).thenReturn(emptyPageResponse);

            mockMvc.perform(get("/api/v1/tenants")
                    .param("page", "0")
                    .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content", hasSize(0)));
        }

        @Test
        void getAllTenants_DefaultPagination() throws Exception {
            TenantResponseDTO tenant = TenantResponseDTO.builder().id(1).name("Test").build();
            PageResponseDTO<TenantResponseDTO> pageResponse = PageResponseDTO.<TenantResponseDTO>builder()
                    .content(List.of(tenant))
                    .number(0)
                    .size(10)
                    .totalElements(1L)
                    .build();

            when(tenantManagementService.getAllTenants(0, 10)).thenReturn(pageResponse);

            mockMvc.perform(get("/api/v1/tenants"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content", hasSize(1)));
        }
    }

    @Nested
    @DisplayName("Update Tenant")
    class UpdateTenantTests {
        @Test
        void updateTenant_Success() throws Exception {
            Integer tenantId = 1;
            UpdateTenantRequestDTO request = new UpdateTenantRequestDTO();
            request.setStatus("INACTIVE");

            TenantResponseDTO response = TenantResponseDTO.builder().id(1).status("INACTIVE").build();

            when(tenantManagementService.updateTenant(eq(tenantId), any(UpdateTenantRequestDTO.class))).thenReturn(response);

            mockMvc.perform(put("/api/v1/tenants/" + tenantId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value(200))
                    .andExpect(jsonPath("$.data.status").value("INACTIVE"));

            verify(tenantManagementService).updateTenant(eq(tenantId), any());
        }

        @Test
        void updateTenant_NotFound() throws Exception {
            Integer tenantId = 999;
            UpdateTenantRequestDTO request = UpdateTenantRequestDTO.builder().status("INACTIVE").build();

            when(tenantManagementService.updateTenant(eq(tenantId), any()))
                    .thenThrow(new ResourceNotFoundException("Tenant not found"));

            mockMvc.perform(put("/api/v1/tenants/" + tenantId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("Deactivate Tenant")
    class DeactivateTenantTests {
        @Test
        void deactivateTenant_Success() throws Exception {
            Integer tenantId = 1;
            doNothing().when(tenantManagementService).deactivateTenant(tenantId);

            mockMvc.perform(put("/api/v1/tenants/" + tenantId + "/deactivate")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value(200))
                    .andExpect(jsonPath("$.message").value("Tenant deactivated successfully"));

            verify(tenantManagementService).deactivateTenant(tenantId);
        }

        @Test
        void deactivateTenant_NotFound() throws Exception {
            Integer tenantId = 999;
            doThrow(new ResourceNotFoundException("Tenant not found"))
                    .when(tenantManagementService).deactivateTenant(tenantId);

            mockMvc.perform(put("/api/v1/tenants/" + tenantId + "/deactivate"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("Get Tenant Configurations")
    class GetTenantConfigsTests {
        @Test
        void getTenantConfigs_Success() throws Exception {
            Integer tenantId = 1;
            Map<TenantConfigKeyEnum, String> configs = new HashMap<>();
            configs.put(TenantConfigKeyEnum.TENANT_LOGO_URL, "url");
            TenantConfigResponseDTO response = TenantConfigResponseDTO.builder().tenantId(tenantId).configs(configs).build();

            when(tenantManagementService.getTenantConfigs(eq(tenantId), any())).thenReturn(response);

            mockMvc.perform(get("/api/v1/tenants/" + tenantId + "/config")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value(200))
                    .andExpect(jsonPath("$.data.configs.TENANT_LOGO_URL").value("url"));

            verify(tenantManagementService).getTenantConfigs(eq(tenantId), any());
        }

        @Test
        void getTenantConfigs_WithKeys() throws Exception {
            Integer tenantId = 1;
            Map<TenantConfigKeyEnum, String> configs = new HashMap<>();
            configs.put(TenantConfigKeyEnum.TENANT_LOGO_URL, "url");
            TenantConfigResponseDTO response = TenantConfigResponseDTO.builder().tenantId(tenantId).configs(configs).build();

            when(tenantManagementService.getTenantConfigs(eq(tenantId), any())).thenReturn(response);

            mockMvc.perform(get("/api/v1/tenants/" + tenantId + "/config")
                    .param("keys", "TENANT_LOGO_URL")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.configs.TENANT_LOGO_URL").value("url"));
        }

        @Test
        void getTenantConfigs_NotFound() throws Exception {
            Integer tenantId = 999;
            when(tenantManagementService.getTenantConfigs(eq(tenantId), any()))
                    .thenThrow(new ResourceNotFoundException("Tenant not found"));

            mockMvc.perform(get("/api/v1/tenants/" + tenantId + "/config"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("Set Tenant Configurations")
    class SetTenantConfigsTests {
        @Test
        void setTenantConfigs_Success() throws Exception {
            Integer tenantId = 1;
            Map<TenantConfigKeyEnum, String> configs = new HashMap<>();
            configs.put(TenantConfigKeyEnum.TENANT_LOGO_URL, "url");
            SetTenantConfigRequestDTO request = SetTenantConfigRequestDTO.builder().configs(configs).build();
            TenantConfigResponseDTO response = TenantConfigResponseDTO.builder().tenantId(tenantId).configs(configs).build();

            when(tenantManagementService.setTenantConfigs(eq(tenantId), any(SetTenantConfigRequestDTO.class)))
                    .thenReturn(response);

            mockMvc.perform(put("/api/v1/tenants/" + tenantId + "/config")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value(200))
                    .andExpect(jsonPath("$.data.configs.TENANT_LOGO_URL").value("url"));

            verify(tenantManagementService).setTenantConfigs(eq(tenantId), any());
        }

        @Test
        void setTenantConfigs_NotFound() throws Exception {
            Integer tenantId = 999;
            Map<TenantConfigKeyEnum, String> configs = new HashMap<>();
            configs.put(TenantConfigKeyEnum.TENANT_LOGO_URL, "url");
            SetTenantConfigRequestDTO request = SetTenantConfigRequestDTO.builder().configs(configs).build();

            when(tenantManagementService.setTenantConfigs(eq(tenantId), any()))
                    .thenThrow(new ResourceNotFoundException("Tenant not found"));

            mockMvc.perform(put("/api/v1/tenants/" + tenantId + "/config")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("Get Tenant Departments")
    class GetTenantDepartmentsTests {
        @Test
        void getTenantDepartments_Success() throws Exception {
            DepartmentResponseDTO dept1 = DepartmentResponseDTO.builder().id(1).title("Health").status(1).build();
            DepartmentResponseDTO dept2 = DepartmentResponseDTO.builder().id(2).title("Operations").status(1).build();

            when(tenantManagementService.getTenantDepartments()).thenReturn(List.of(dept1, dept2));

            mockMvc.perform(get("/api/v1/tenants/departments")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value(200))
                    .andExpect(jsonPath("$.data", hasSize(2)));

            verify(tenantManagementService).getTenantDepartments();
        }

        @Test
        void getTenantDepartments_EmptyList() throws Exception {
            when(tenantManagementService.getTenantDepartments()).thenReturn(Collections.emptyList());

            mockMvc.perform(get("/api/v1/tenants/departments"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data", hasSize(0)));
        }
    }

    @Nested
    @DisplayName("Create Department")
    class CreateDepartmentTests {
        @Test
        void createDepartment_Success() throws Exception {
            CreateDepartmentRequestDTO request = new CreateDepartmentRequestDTO();
            request.setTitle("Health");
            request.setStatus(1);
            request.setParentId(0);

            DepartmentResponseDTO dept = DepartmentResponseDTO.builder().id(1).title("Health").build();

            when(tenantManagementService.createDepartment(any(CreateDepartmentRequestDTO.class))).thenReturn(dept);

            mockMvc.perform(post("/api/v1/tenants/departments")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.status").value(201))
                    .andExpect(jsonPath("$.data.title").value("Health"));

            verify(tenantManagementService).createDepartment(any());
        }

        @Test
        void createDepartment_MissingTitle() throws Exception {
            String requestJson = "{\"status\":1,\"parentId\":0}";

            mockMvc.perform(post("/api/v1/tenants/departments")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestJson))
                    .andExpect(status().is4xxClientError());

            verify(tenantManagementService, never()).createDepartment(any());
        }
    }
}
