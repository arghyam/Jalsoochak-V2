package org.arghyam.jalsoochak.tenant.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.arghyam.jalsoochak.tenant.dto.common.PageResponseDTO;
import org.arghyam.jalsoochak.tenant.dto.internal.ConfigValueDTO;
import org.arghyam.jalsoochak.tenant.dto.internal.LocationLevelConfigDTO;
import org.arghyam.jalsoochak.tenant.dto.internal.LocationLevelNameDTO;
import org.arghyam.jalsoochak.tenant.dto.internal.SimpleConfigValueDTO;
import org.arghyam.jalsoochak.tenant.dto.request.CreateDepartmentRequestDTO;
import org.arghyam.jalsoochak.tenant.dto.request.CreateTenantRequestDTO;
import org.arghyam.jalsoochak.tenant.dto.request.SetTenantConfigRequestDTO;
import org.arghyam.jalsoochak.tenant.dto.request.UpdateTenantRequestDTO;
import org.arghyam.jalsoochak.tenant.dto.response.DepartmentResponseDTO;
import org.arghyam.jalsoochak.tenant.dto.response.LocationHierarchyEditConstraintsResponseDTO;
import org.arghyam.jalsoochak.tenant.dto.response.LocationHierarchyResponseDTO;
import org.arghyam.jalsoochak.tenant.dto.response.LocationResponseDTO;
import org.arghyam.jalsoochak.tenant.dto.response.TenantConfigResponseDTO;
import org.arghyam.jalsoochak.tenant.dto.response.TenantResponseDTO;
import org.arghyam.jalsoochak.tenant.enums.TenantConfigKeyEnum;
import org.arghyam.jalsoochak.tenant.exception.InvalidConfigKeyException;
import org.arghyam.jalsoochak.tenant.exception.LocationHierarchyStructureLockedException;
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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

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
                    .name("Karnataka")
                    .stateCode("KA")
                    .lgdCode(29)
                    .build();

            when(tenantManagementService.createTenant(any()))
                    .thenThrow(new IllegalStateException("Tenant already exists for stateCode: KA"));

            mockMvc.perform(post("/api/v1/tenants")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status").value(409));

            verify(tenantManagementService).createTenant(any());
        }

        @Test
        void createTenant_InternalError() throws Exception {
            CreateTenantRequestDTO request = CreateTenantRequestDTO.builder()
                    .name("Karnataka")
                    .stateCode("KA")
                    .lgdCode(29)
                    .build();

            when(tenantManagementService.createTenant(any()))
                    .thenThrow(new RuntimeException("Schema provisioning failed"));

            mockMvc.perform(post("/api/v1/tenants")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isInternalServerError());
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
            request.setStatus("ACTIVE");

            TenantResponseDTO response = TenantResponseDTO.builder().id(1).status("ACTIVE").build();

            when(tenantManagementService.updateTenant(eq(tenantId), any(UpdateTenantRequestDTO.class))).thenReturn(response);

            mockMvc.perform(put("/api/v1/tenants/" + tenantId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value(200))
                    .andExpect(jsonPath("$.data.status").value("ACTIVE"));

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
            Map<TenantConfigKeyEnum, ConfigValueDTO> configs = new HashMap<>();
            configs.put(TenantConfigKeyEnum.TENANT_LOGO, new SimpleConfigValueDTO("url"));
            TenantConfigResponseDTO response = TenantConfigResponseDTO.builder().tenantId(tenantId).configs(configs).build();

            when(tenantManagementService.getTenantConfigs(eq(tenantId), any())).thenReturn(response);

            mockMvc.perform(get("/api/v1/tenants/" + tenantId + "/config")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value(200))
                    .andExpect(jsonPath("$.data.configs.TENANT_LOGO.value").value("url"));

            verify(tenantManagementService).getTenantConfigs(eq(tenantId), any());
        }

        @Test
        void getTenantConfigs_WithKeys() throws Exception {
            Integer tenantId = 1;
            Map<TenantConfigKeyEnum, ConfigValueDTO> configs = new HashMap<>();
            configs.put(TenantConfigKeyEnum.TENANT_LOGO, new SimpleConfigValueDTO("url"));
            TenantConfigResponseDTO response = TenantConfigResponseDTO.builder().tenantId(tenantId).configs(configs).build();

            when(tenantManagementService.getTenantConfigs(eq(tenantId), any())).thenReturn(response);

            mockMvc.perform(get("/api/v1/tenants/" + tenantId + "/config")
                    .param("keys", "TENANT_LOGO")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.configs.TENANT_LOGO.value").value("url"));
        }

        @Test
        void getTenantConfigs_NotFound() throws Exception {
            Integer tenantId = 999;
            when(tenantManagementService.getTenantConfigs(eq(tenantId), any()))
                    .thenThrow(new ResourceNotFoundException("Tenant not found"));

            mockMvc.perform(get("/api/v1/tenants/" + tenantId + "/config"))
                    .andExpect(status().isNotFound());
        }

        @Test
        void getTenantConfigs_InvalidKey_ReturnsBadRequest() throws Exception {
            Integer tenantId = 1;

            mockMvc.perform(get("/api/v1/tenants/" + tenantId + "/config")
                    .param("keys", "NOT_A_VALID_KEY"))
                    .andExpect(status().isBadRequest());

            verify(tenantManagementService, never()).getTenantConfigs(any(), any());
        }
    }

    @Nested
    @DisplayName("Set Tenant Configurations")
    class SetTenantConfigsTests {
        @Test
        void setTenantConfigs_Success() throws Exception {
            Integer tenantId = 1;
            Map<TenantConfigKeyEnum, JsonNode> requestConfigs = new HashMap<>();
            requestConfigs.put(TenantConfigKeyEnum.TENANT_LOGO, objectMapper.readTree("{\"value\":\"url\"}"));
            SetTenantConfigRequestDTO request = SetTenantConfigRequestDTO.builder().configs(requestConfigs).build();
            
            Map<TenantConfigKeyEnum, ConfigValueDTO> responseConfigs = new HashMap<>();
            responseConfigs.put(TenantConfigKeyEnum.TENANT_LOGO, new SimpleConfigValueDTO("url"));
            TenantConfigResponseDTO response = TenantConfigResponseDTO.builder().tenantId(tenantId).configs(responseConfigs).build();

            when(tenantManagementService.setTenantConfigs(eq(tenantId), any(SetTenantConfigRequestDTO.class)))
                    .thenReturn(response);

            mockMvc.perform(put("/api/v1/tenants/" + tenantId + "/config")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value(200))
                    .andExpect(jsonPath("$.data.configs.TENANT_LOGO.value").value("url"));

            verify(tenantManagementService).setTenantConfigs(eq(tenantId), any());
        }

        @Test
        void setTenantConfigs_InvalidConfigKey_ReturnsBadRequest() throws Exception {
            Integer tenantId = 1;
            Map<TenantConfigKeyEnum, JsonNode> configs = new HashMap<>();
            configs.put(TenantConfigKeyEnum.TENANT_LOGO, objectMapper.readTree("{\"value\":\"url\"}"));
            SetTenantConfigRequestDTO request = SetTenantConfigRequestDTO.builder().configs(configs).build();

            when(tenantManagementService.setTenantConfigs(eq(tenantId), any()))
                    .thenThrow(new InvalidConfigKeyException("Unknown config key"));

            mockMvc.perform(put("/api/v1/tenants/" + tenantId + "/config")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void setTenantConfigs_NotFound() throws Exception {
            Integer tenantId = 999;
            Map<TenantConfigKeyEnum, JsonNode> configs = new HashMap<>();
            configs.put(TenantConfigKeyEnum.TENANT_LOGO, objectMapper.readTree("{\"value\":\"url\"}"));
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

    @Nested
    @DisplayName("Location Hierarchy")
    class LocationHierarchyTests {
        @Test
        void getLocationHierarchy_Success() throws Exception {
            Integer tenantId = 1;
            String hierarchyType = "LGD";

            List<LocationLevelConfigDTO> levels = List.of(
                    LocationLevelConfigDTO.builder().level(1)
                            .levelName(List.of(LocationLevelNameDTO.builder().title("State").build())).build(),
                    LocationLevelConfigDTO.builder().level(2)
                            .levelName(List.of(LocationLevelNameDTO.builder().title("District").build())).build()
            );

            LocationHierarchyResponseDTO hierarchy = LocationHierarchyResponseDTO.builder()
                    .hierarchyType(hierarchyType)
                    .levels(levels)
                    .build();

            when(tenantManagementService.getLocationHierarchy(tenantId, hierarchyType)).thenReturn(hierarchy);

            mockMvc.perform(get("/api/v1/tenants/{tenantId}/location-hierarchy/{hierarchyType}", tenantId, hierarchyType))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value(200))
                    .andExpect(jsonPath("$.data.hierarchyType").value("LGD"))
                    .andExpect(jsonPath("$.data.levels", hasSize(2)))
                    .andExpect(jsonPath("$.data.levels[0].levelName[0].title").value("State"))
                    .andExpect(jsonPath("$.data.levels[1].levelName[0].title").value("District"));

            verify(tenantManagementService).getLocationHierarchy(tenantId, hierarchyType);
        }

        @Test
        void getLocationHierarchy_DepartmentType_Success() throws Exception {
            Integer tenantId = 1;
            String hierarchyType = "DEPARTMENT";

            List<LocationLevelConfigDTO> levels = List.of(
                    LocationLevelConfigDTO.builder().level(1)
                            .levelName(List.of(LocationLevelNameDTO.builder().title("Zone").build())).build(),
                    LocationLevelConfigDTO.builder().level(2)
                            .levelName(List.of(LocationLevelNameDTO.builder().title("Circle").build())).build(),
                    LocationLevelConfigDTO.builder().level(3)
                            .levelName(List.of(LocationLevelNameDTO.builder().title("Division").build())).build()
            );

            LocationHierarchyResponseDTO hierarchy = LocationHierarchyResponseDTO.builder()
                    .hierarchyType(hierarchyType)
                    .levels(levels)
                    .build();

            when(tenantManagementService.getLocationHierarchy(tenantId, hierarchyType)).thenReturn(hierarchy);

            mockMvc.perform(get("/api/v1/tenants/{tenantId}/location-hierarchy/{hierarchyType}", tenantId, hierarchyType))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.hierarchyType").value("DEPARTMENT"))
                    .andExpect(jsonPath("$.data.levels", hasSize(3)));
        }

        @Test
        void getLocationHierarchy_InvalidType_ReturnsBadRequest() throws Exception {
            Integer tenantId = 1;
            String hierarchyType = "INVALID_TYPE";

            when(tenantManagementService.getLocationHierarchy(tenantId, hierarchyType))
                    .thenThrow(new IllegalArgumentException("Invalid hierarchy type: INVALID_TYPE"));

            mockMvc.perform(get("/api/v1/tenants/{tenantId}/location-hierarchy/{hierarchyType}", tenantId, hierarchyType))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void getLocationHierarchy_ResourceNotFound() throws Exception {
            Integer tenantId = 1;
            String hierarchyType = "LGD";

            when(tenantManagementService.getLocationHierarchy(tenantId, hierarchyType))
                    .thenThrow(new ResourceNotFoundException("Hierarchy not found"));

            mockMvc.perform(get("/api/v1/tenants/{tenantId}/location-hierarchy/{hierarchyType}", tenantId, hierarchyType))
                    .andExpect(status().isNotFound());

            verify(tenantManagementService).getLocationHierarchy(tenantId, hierarchyType);
        }

        @Test
        void getLocationChildren_Success() throws Exception {
            Integer tenantId = 1;
            String hierarchyType = "LGD";
            Integer parentId = 0;

            List<LocationResponseDTO> children = List.of(
                    LocationResponseDTO.builder().id(1).uuid("uuid-1").title("Madhya Pradesh").status(1).build(),
                    LocationResponseDTO.builder().id(2).uuid("uuid-2").title("Maharashtra").status(1).build()
            );

            when(tenantManagementService.getLocationChildren(tenantId, hierarchyType, null)).thenReturn(children);

            mockMvc.perform(get("/api/v1/tenants/{tenantId}/locations/{hierarchyType}/children/{parentId}", tenantId, hierarchyType, parentId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value(200))
                    .andExpect(jsonPath("$.data", hasSize(2)))
                    .andExpect(jsonPath("$.data[0].title").value("Madhya Pradesh"))
                    .andExpect(jsonPath("$.data[1].title").value("Maharashtra"));

            verify(tenantManagementService).getLocationChildren(tenantId, hierarchyType, null);
        }

        @Test
        void getLocationChildren_WithParentId() throws Exception {
            Integer tenantId = 1;
            String hierarchyType = "LGD";
            Integer parentId = 1;

            List<LocationResponseDTO> children = List.of(
                    LocationResponseDTO.builder().id(10).uuid("uuid-10").title("Indore").lgdCode("IND001").parentId(1).status(1).build(),
                    LocationResponseDTO.builder().id(11).uuid("uuid-11").title("Bhopal").lgdCode("BHP001").parentId(1).status(1).build()
            );

            when(tenantManagementService.getLocationChildren(tenantId, hierarchyType, parentId)).thenReturn(children);

            mockMvc.perform(get("/api/v1/tenants/{tenantId}/locations/{hierarchyType}/children/{parentId}", tenantId, hierarchyType, parentId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value(200))
                    .andExpect(jsonPath("$.data", hasSize(2)))
                    .andExpect(jsonPath("$.data[0].title").value("Indore"))
                    .andExpect(jsonPath("$.data[0].parentId").value(1));

            verify(tenantManagementService).getLocationChildren(tenantId, hierarchyType, parentId);
        }

        @Test
        void getLocationChildren_EmptyResult() throws Exception {
            Integer tenantId = 1;
            String hierarchyType = "DEPARTMENT";
            Integer parentId = 999;

            when(tenantManagementService.getLocationChildren(tenantId, hierarchyType, parentId)).thenReturn(new ArrayList<>());

            mockMvc.perform(get("/api/v1/tenants/{tenantId}/locations/{hierarchyType}/children/{parentId}", tenantId, hierarchyType, parentId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value(200))
                    .andExpect(jsonPath("$.data", hasSize(0)));

            verify(tenantManagementService).getLocationChildren(tenantId, hierarchyType, parentId);
        }

        @Test
        void getLocationChildren_InvalidHierarchyType() throws Exception {
            Integer tenantId = 1;
            String hierarchyType = "INVALID";
            Integer parentId = 0;

            when(tenantManagementService.getLocationChildren(tenantId, hierarchyType, null))
                    .thenThrow(new IllegalArgumentException("Invalid hierarchy type: " + hierarchyType));

            mockMvc.perform(get("/api/v1/tenants/{tenantId}/locations/{hierarchyType}/children/{parentId}", tenantId, hierarchyType, parentId))
                    .andExpect(status().isBadRequest());

            verify(tenantManagementService).getLocationChildren(tenantId, hierarchyType, null);
        }

        @Test
        void getLocationHierarchyEditConstraints_StructuralAllowed() throws Exception {
            Integer tenantId = 1;
            String hierarchyType = "LGD";

            LocationHierarchyEditConstraintsResponseDTO constraints =
                    LocationHierarchyEditConstraintsResponseDTO.builder()
                            .hierarchyType("LGD")
                            .structuralChangesAllowed(true)
                            .seededRecordCount(0L)
                            .build();

            when(tenantManagementService.getLocationHierarchyEditConstraints(tenantId, hierarchyType))
                    .thenReturn(constraints);

            mockMvc.perform(get("/api/v1/tenants/{tenantId}/location-hierarchy/{hierarchyType}/edit-constraints",
                            tenantId, hierarchyType))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value(200))
                    .andExpect(jsonPath("$.data.hierarchyType").value("LGD"))
                    .andExpect(jsonPath("$.data.structuralChangesAllowed").value(true))
                    .andExpect(jsonPath("$.data.seededRecordCount").value(0));

            verify(tenantManagementService).getLocationHierarchyEditConstraints(tenantId, hierarchyType);
        }

        @Test
        void getLocationHierarchyEditConstraints_StructuralLocked() throws Exception {
            Integer tenantId = 1;
            String hierarchyType = "DEPARTMENT";

            LocationHierarchyEditConstraintsResponseDTO constraints =
                    LocationHierarchyEditConstraintsResponseDTO.builder()
                            .hierarchyType("DEPARTMENT")
                            .structuralChangesAllowed(false)
                            .seededRecordCount(512L)
                            .build();

            when(tenantManagementService.getLocationHierarchyEditConstraints(tenantId, hierarchyType))
                    .thenReturn(constraints);

            mockMvc.perform(get("/api/v1/tenants/{tenantId}/location-hierarchy/{hierarchyType}/edit-constraints",
                            tenantId, hierarchyType))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.structuralChangesAllowed").value(false))
                    .andExpect(jsonPath("$.data.seededRecordCount").value(512));
        }

        @Test
        void getLocationHierarchyEditConstraints_TenantNotFound() throws Exception {
            Integer tenantId = 999;
            when(tenantManagementService.getLocationHierarchyEditConstraints(tenantId, "LGD"))
                    .thenThrow(new ResourceNotFoundException("Tenant not found"));

            mockMvc.perform(get("/api/v1/tenants/{tenantId}/location-hierarchy/{hierarchyType}/edit-constraints",
                            tenantId, "LGD"))
                    .andExpect(status().isNotFound());
        }

        @Test
        void getLocationHierarchyEditConstraints_InvalidType() throws Exception {
            Integer tenantId = 1;
            when(tenantManagementService.getLocationHierarchyEditConstraints(tenantId, "INVALID"))
                    .thenThrow(new IllegalArgumentException("Invalid hierarchy type: INVALID"));

            mockMvc.perform(get("/api/v1/tenants/{tenantId}/location-hierarchy/{hierarchyType}/edit-constraints",
                            tenantId, "INVALID"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void updateLocationHierarchy_Success() throws Exception {
            Integer tenantId = 1;
            String hierarchyType = "LGD";

            List<LocationLevelConfigDTO> levels = List.of(
                    LocationLevelConfigDTO.builder().level(1)
                            .levelName(List.of(LocationLevelNameDTO.builder().title("Rajya").build())).build(),
                    LocationLevelConfigDTO.builder().level(2)
                            .levelName(List.of(LocationLevelNameDTO.builder().title("Zila").build())).build()
            );

            LocationHierarchyResponseDTO response = LocationHierarchyResponseDTO.builder()
                    .hierarchyType("LGD")
                    .levels(levels)
                    .build();

            when(tenantManagementService.updateLocationHierarchy(eq(tenantId), eq(hierarchyType), any()))
                    .thenReturn(response);

            mockMvc.perform(put("/api/v1/tenants/{tenantId}/location-hierarchy/{hierarchyType}",
                            tenantId, hierarchyType)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(levels)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value(200))
                    .andExpect(jsonPath("$.data.hierarchyType").value("LGD"))
                    .andExpect(jsonPath("$.data.levels", hasSize(2)));

            verify(tenantManagementService).updateLocationHierarchy(eq(tenantId), eq(hierarchyType), any());
        }

        @Test
        void updateLocationHierarchy_StructureLocked_Returns409() throws Exception {
            Integer tenantId = 1;
            String hierarchyType = "LGD";

            List<LocationLevelConfigDTO> levels = List.of(
                    LocationLevelConfigDTO.builder().level(1)
                            .levelName(List.of(LocationLevelNameDTO.builder().title("State").build())).build(),
                    LocationLevelConfigDTO.builder().level(2)
                            .levelName(List.of(LocationLevelNameDTO.builder().title("District").build())).build()
            );

            when(tenantManagementService.updateLocationHierarchy(eq(tenantId), eq(hierarchyType), any()))
                    .thenThrow(new LocationHierarchyStructureLockedException("LGD", 1842L));

            mockMvc.perform(put("/api/v1/tenants/{tenantId}/location-hierarchy/{hierarchyType}",
                            tenantId, hierarchyType)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(levels)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status").value(409));
        }

        @Test
        void updateLocationHierarchy_TenantNotFound() throws Exception {
            Integer tenantId = 999;
            List<LocationLevelConfigDTO> levels = List.of(
                    LocationLevelConfigDTO.builder().level(1)
                            .levelName(List.of(LocationLevelNameDTO.builder().title("State").build())).build()
            );

            when(tenantManagementService.updateLocationHierarchy(eq(tenantId), eq("LGD"), any()))
                    .thenThrow(new ResourceNotFoundException("Tenant not found"));

            mockMvc.perform(put("/api/v1/tenants/{tenantId}/location-hierarchy/{hierarchyType}",
                            tenantId, "LGD")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(levels)))
                    .andExpect(status().isNotFound());
        }
    }
}
