package org.arghyam.jalsoochak.tenant.service.serviceImpl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.arghyam.jalsoochak.tenant.config.TenantContext;
import org.arghyam.jalsoochak.tenant.config.TenantDefaultsProperties;
import org.arghyam.jalsoochak.tenant.dto.common.PageResponseDTO;
import org.arghyam.jalsoochak.tenant.dto.internal.ConfigDTO;
import org.arghyam.jalsoochak.tenant.dto.internal.ConfigValueDTO;
import org.arghyam.jalsoochak.tenant.dto.internal.LocationConfigDTO;
import org.arghyam.jalsoochak.tenant.dto.internal.LocationLevelConfigDTO;
import org.arghyam.jalsoochak.tenant.dto.internal.LocationLevelNameDTO;
import org.arghyam.jalsoochak.tenant.dto.internal.ReasonItemDTO;
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
import org.arghyam.jalsoochak.tenant.enums.RegionTypeEnum;
import org.arghyam.jalsoochak.tenant.enums.TenantConfigKeyEnum;
import org.arghyam.jalsoochak.tenant.event.TenantCreatedEvent;
import org.arghyam.jalsoochak.tenant.event.TenantDeactivatedEvent;
import org.arghyam.jalsoochak.tenant.event.TenantUpdatedEvent;
import org.arghyam.jalsoochak.tenant.exception.LocationHierarchyStructureLockedException;
import org.arghyam.jalsoochak.tenant.exception.ResourceNotFoundException;
import org.arghyam.jalsoochak.tenant.repository.TenantCommonRepository;
import org.arghyam.jalsoochak.tenant.repository.TenantSchemaRepository;
import org.arghyam.jalsoochak.tenant.service.TenantSchedulerManager;
import org.arghyam.jalsoochak.tenant.util.SecurityUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Comprehensive unit tests for TenantManagementServiceImpl.
 * Covers success paths, error scenarios, edge cases, and integration with dependencies.
 * Follows AAA (Arrange-Act-Assert) pattern and includes detailed test method names.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Tenant Management Service - Comprehensive Tests")
class TenantManagementServiceImplTest {

    @Mock
    private TenantCommonRepository tenantCommonRepository;

    @Mock
    private TenantSchemaRepository tenantSchemaRepository;

    @Mock
    private TenantDefaultsProperties tenantDefaults;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private TenantSchedulerManager schedulerManager;

    private ObjectMapper objectMapper;

    private TenantManagementServiceImpl tenantManagementService;

    private MockedStatic<SecurityUtils> mockedSecurityUtils;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        mockedSecurityUtils = mockStatic(SecurityUtils.class);
        lenient().when(tenantDefaults.getLgdLocationHierarchy()).thenReturn(Collections.emptyList());
        lenient().when(tenantDefaults.getDeptLocationHierarchy()).thenReturn(Collections.emptyList());
        lenient().when(tenantDefaults.getMeterChangeReasons()).thenReturn(Collections.emptyList());
        
        // Manually create service with real ObjectMapper and mocked dependencies
        tenantManagementService = new TenantManagementServiceImpl(
            tenantCommonRepository,
            tenantSchemaRepository,
            objectMapper,
            tenantDefaults,
            eventPublisher,
            schedulerManager
        );
    }

    @AfterEach
    void tearDown() {
        mockedSecurityUtils.close();
    }

    @Nested
    @DisplayName("Create Tenant Tests")
    class CreateTenantTests {

        @Test
        @DisplayName("Should create tenant successfully with all valid data")
        void testCreateTenant_Success() throws Exception {
            // Arrange
            CreateTenantRequestDTO request = CreateTenantRequestDTO.builder()
                    .name("Test Tenant")
                    .stateCode("TT")
                    .lgdCode(123)
                    .build();

            TenantResponseDTO expectedResponse = TenantResponseDTO.builder()
                    .id(1)
                    .name("Test Tenant")
                    .stateCode("TT")
                    .status("ACTIVE")
                    .build();

            when(tenantCommonRepository.findByStateCode("TT")).thenReturn(Optional.empty());
            when(SecurityUtils.getCurrentUserUuid()).thenReturn("user-uuid");
            when(tenantCommonRepository.findUserIdByUuid("user-uuid")).thenReturn(Optional.of(100));
            when(tenantCommonRepository.createTenant(any(CreateTenantRequestDTO.class), anyInt()))
                    .thenReturn(Optional.of(expectedResponse));
            when(tenantCommonRepository.upsertConfig(eq(1), eq("METER_CHANGE_REASONS"), anyString(), eq(100)))
                    .thenReturn(Optional.of(ConfigDTO.builder().build()));
            when(tenantCommonRepository.upsertConfig(eq(1), eq("LOCATION_CHECK_REQUIRED"), anyString(), eq(100)))
                    .thenReturn(Optional.of(ConfigDTO.builder().build()));

            // Act
            TenantResponseDTO result = tenantManagementService.createTenant(request);

            // Assert
            assertNotNull(result);
            assertEquals("Test Tenant", result.getName());
            assertEquals("TT", result.getStateCode());
            assertEquals("ACTIVE", result.getStatus());
            
            verify(tenantCommonRepository).findByStateCode("TT");
            verify(tenantCommonRepository).createTenant(eq(request), eq(100));
            verify(tenantCommonRepository).provisionTenantSchema("tenant_tt");
            verify(eventPublisher).publishEvent(any(TenantCreatedEvent.class));
        }

        @Test
        @DisplayName("Should throw exception when tenant with same state code already exists")
        void testCreateTenant_DuplicateStateCode() {
            // Arrange
            CreateTenantRequestDTO request = CreateTenantRequestDTO.builder()
                    .stateCode("TT")
                    .build();

            TenantResponseDTO existing = TenantResponseDTO.builder().build();
            when(tenantCommonRepository.findByStateCode("TT"))
                    .thenReturn(Optional.of(existing));

            // Act & Assert
            assertThrows(IllegalStateException.class, 
                    () -> tenantManagementService.createTenant(request));
            
            verify(tenantCommonRepository).findByStateCode("TT");
            verify(tenantCommonRepository, never()).createTenant(any(), any());
        }

        @Test
        @DisplayName("Should handle tenant creation failure gracefully")
        void testCreateTenant_CreationFailed() {
            // Arrange
            CreateTenantRequestDTO request = CreateTenantRequestDTO.builder()
                    .stateCode("TT")
                    .build();

            when(tenantCommonRepository.findByStateCode("TT")).thenReturn(Optional.empty());
            when(SecurityUtils.getCurrentUserUuid()).thenReturn("user-uuid");
            when(tenantCommonRepository.findUserIdByUuid("user-uuid")).thenReturn(Optional.of(100));
            when(tenantCommonRepository.createTenant(any(), any()))
                    .thenReturn(Optional.empty());

            // Act & Assert
            assertThrows(RuntimeException.class,
                    () -> tenantManagementService.createTenant(request));

            verify(tenantCommonRepository).createTenant(any(), any());
        }

        @Test
        @DisplayName("Should seed default configs for new tenant after creation")
        void testCreateTenant_SeedsDefaultConfigs() throws Exception {
            // Arrange
            CreateTenantRequestDTO request = CreateTenantRequestDTO.builder()
                    .name("Test Tenant")
                    .stateCode("TT")
                    .lgdCode(123)
                    .build();

            TenantResponseDTO tenant = TenantResponseDTO.builder()
                    .id(1)
                    .name("Test Tenant")
                    .stateCode("TT")
                    .status("ACTIVE")
                    .build();

            List<LocationLevelConfigDTO> lgdLevels = List.of(
                    LocationLevelConfigDTO.builder().level(1)
                            .levelName(List.of(LocationLevelNameDTO.builder().title("State").build())).build(),
                    LocationLevelConfigDTO.builder().level(2)
                            .levelName(List.of(LocationLevelNameDTO.builder().title("District").build())).build()
            );
            List<LocationLevelConfigDTO> deptLevels = List.of(
                    LocationLevelConfigDTO.builder().level(1)
                            .levelName(List.of(LocationLevelNameDTO.builder().title("Zone").build())).build()
            );
            List<ReasonItemDTO> reasons = List.of(
                    ReasonItemDTO.builder().id("METER_REPLACED").name("Meter Replaced")
                            .sequenceOrder(1).isDefault(true).editable(true).build()
            );

            when(tenantCommonRepository.findByStateCode("TT")).thenReturn(Optional.empty());
            when(SecurityUtils.getCurrentUserUuid()).thenReturn("user-uuid");
            when(tenantCommonRepository.findUserIdByUuid("user-uuid")).thenReturn(Optional.of(100));
            when(tenantCommonRepository.createTenant(any(CreateTenantRequestDTO.class), anyInt()))
                    .thenReturn(Optional.of(tenant));
            when(tenantDefaults.getLgdLocationHierarchy()).thenReturn(lgdLevels);
            when(tenantDefaults.getDeptLocationHierarchy()).thenReturn(deptLevels);
            when(tenantDefaults.getMeterChangeReasons()).thenReturn(reasons);
            when(tenantCommonRepository.upsertConfig(eq(1), eq("METER_CHANGE_REASONS"), anyString(), eq(100)))
                    .thenReturn(Optional.of(ConfigDTO.builder().build()));
            when(tenantCommonRepository.upsertConfig(eq(1), eq("LOCATION_CHECK_REQUIRED"), anyString(), eq(100)))
                    .thenReturn(Optional.of(ConfigDTO.builder().build()));

            // Act
            tenantManagementService.createTenant(request);

            // Assert — both hierarchy types seeded
            verify(tenantSchemaRepository).setLocationHierarchy("tenant_tt", RegionTypeEnum.LGD, lgdLevels, 100);
            verify(tenantSchemaRepository).setLocationHierarchy("tenant_tt", RegionTypeEnum.DEPARTMENT, deptLevels, 100);
            // METER_CHANGE_REASONS and LOCATION_CHECK_REQUIRED written via upsertConfig
            verify(tenantCommonRepository).upsertConfig(eq(1), eq("METER_CHANGE_REASONS"), anyString(), eq(100));
            verify(tenantCommonRepository).upsertConfig(eq(1), eq("LOCATION_CHECK_REQUIRED"), anyString(), eq(100));
        }
    }

    @Nested
    @DisplayName("Update Tenant Tests")
    class UpdateTenantTests {

        @Test
        @DisplayName("Should update tenant successfully")
        void testUpdateTenant_Success() throws Exception {
            // Arrange
            Integer tenantId = 1;
            UpdateTenantRequestDTO request = new UpdateTenantRequestDTO();
            request.setStatus("ACTIVE");

            TenantResponseDTO existing = TenantResponseDTO.builder()
                    .id(tenantId).stateCode("TT").name("Tenant").status("ACTIVE").build();
            TenantResponseDTO updated = TenantResponseDTO.builder()
                    .id(tenantId).stateCode("TT").name("Tenant").status("ACTIVE").build();

            when(tenantCommonRepository.findById(tenantId)).thenReturn(Optional.of(existing));
            when(SecurityUtils.getCurrentUserUuid()).thenReturn("user-uuid");
            when(tenantCommonRepository.findUserIdByUuid("user-uuid")).thenReturn(Optional.of(100));
            when(tenantCommonRepository.updateTenant(anyInt(), any(UpdateTenantRequestDTO.class), anyInt()))
                    .thenReturn(Optional.of(updated));

            // Act
            TenantResponseDTO result = tenantManagementService.updateTenant(tenantId, request);

            // Assert
            assertNotNull(result);
            assertEquals("ACTIVE", result.getStatus());
            verify(tenantCommonRepository).updateTenant(anyInt(), any(UpdateTenantRequestDTO.class), anyInt());
            verify(eventPublisher).publishEvent(any(TenantUpdatedEvent.class));
        }

        @Test
        @DisplayName("Should throw exception when trying to deactivate via update endpoint")
        void testUpdateTenant_CannotDeactivate() {
            // Arrange
            Integer tenantId = 1;
            UpdateTenantRequestDTO request = new UpdateTenantRequestDTO();
            request.setStatus("INACTIVE");

            TenantResponseDTO existing = TenantResponseDTO.builder().build();
            when(tenantCommonRepository.findById(tenantId)).thenReturn(Optional.of(existing));

            // Act & Assert
            assertThrows(IllegalArgumentException.class, 
                    () -> tenantManagementService.updateTenant(tenantId, request));
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when tenant not found")
        void testUpdateTenant_NotFound() {
            // Arrange
            Integer tenantId = 999;
            UpdateTenantRequestDTO request = UpdateTenantRequestDTO.builder().status("ACTIVE").build();

            when(tenantCommonRepository.findById(tenantId)).thenReturn(Optional.empty());

            // Act & Assert
            assertThrows(ResourceNotFoundException.class,
                    () -> tenantManagementService.updateTenant(tenantId, request));
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when update returns empty optional")
        void testUpdateTenant_UpdateReturnsEmpty() {
            // Arrange
            Integer tenantId = 1;
            UpdateTenantRequestDTO request = UpdateTenantRequestDTO.builder().status("ACTIVE").build();
            TenantResponseDTO existing = TenantResponseDTO.builder().id(tenantId).build();

            when(tenantCommonRepository.findById(tenantId)).thenReturn(Optional.of(existing));
            when(SecurityUtils.getCurrentUserUuid()).thenReturn("user-uuid");
            when(tenantCommonRepository.findUserIdByUuid("user-uuid")).thenReturn(Optional.of(100));
            when(tenantCommonRepository.updateTenant(anyInt(), any(), anyInt())).thenReturn(Optional.empty());

            // Act & Assert
            assertThrows(ResourceNotFoundException.class,
                    () -> tenantManagementService.updateTenant(tenantId, request));
        }
    }

    @Nested
    @DisplayName("Deactivate Tenant Tests")
    class DeactivateTenantTests {

        @Test
        @DisplayName("Should deactivate tenant successfully")
        void testDeactivateTenant_Success() throws Exception {
            // Arrange
            Integer tenantId = 1;
            TenantResponseDTO existing = TenantResponseDTO.builder()
                    .id(tenantId).stateCode("TT").name("Tenant").status("ACTIVE").build();
            TenantResponseDTO deactivated = TenantResponseDTO.builder()
                    .id(tenantId).stateCode("TT").name("Tenant").status("INACTIVE").build();

            when(tenantCommonRepository.findById(tenantId))
                    .thenReturn(Optional.of(existing))
                    .thenReturn(Optional.of(deactivated));
            when(SecurityUtils.getCurrentUserUuid()).thenReturn("user-uuid");
            when(tenantCommonRepository.findUserIdByUuid("user-uuid")).thenReturn(Optional.of(100));

            // Act
            tenantManagementService.deactivateTenant(tenantId);

            // Assert
            verify(tenantCommonRepository).deactivateTenant(anyInt(), anyInt());
            verify(eventPublisher).publishEvent(any(TenantDeactivatedEvent.class));
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when tenant not found")
        void testDeactivateTenant_NotFound() {
            // Arrange
            Integer tenantId = 999;
            when(tenantCommonRepository.findById(tenantId)).thenReturn(Optional.empty());

            // Act & Assert
            assertThrows(ResourceNotFoundException.class, 
                    () -> tenantManagementService.deactivateTenant(tenantId));
        }
    }

    @Nested
    @DisplayName("Get All Tenants Tests")
    class GetAllTenantsTests {

        @Test
        @DisplayName("Should retrieve all tenants with pagination")
        void testGetAllTenants_Success() {
            // Arrange
            int page = 0;
            int size = 10;
            List<TenantResponseDTO> tenants = List.of(
                    TenantResponseDTO.builder().id(1).name("Tenant1").build(),
                    TenantResponseDTO.builder().id(2).name("Tenant2").build()
            );

            when(tenantCommonRepository.findAll(size, 0)).thenReturn(tenants);
            when(tenantCommonRepository.countAllTenants()).thenReturn(2L);

            // Act
            PageResponseDTO<TenantResponseDTO> result = tenantManagementService.getAllTenants(page, size);

            // Assert
            assertNotNull(result);
            assertEquals(2, result.getContent().size());
            assertEquals(2L, result.getTotalElements());
            verify(tenantCommonRepository).findAll(size, 0);
            verify(tenantCommonRepository).countAllTenants();
        }

        @Test
        @DisplayName("Should return empty list when no tenants exist")
        void testGetAllTenants_Empty() {
            // Arrange
            when(tenantCommonRepository.findAll(10, 0)).thenReturn(Collections.emptyList());
            when(tenantCommonRepository.countAllTenants()).thenReturn(0L);

            // Act
            PageResponseDTO<TenantResponseDTO> result = tenantManagementService.getAllTenants(0, 10);

            // Assert
            assertNotNull(result);
            assertEquals(0, result.getContent().size());
            assertEquals(0L, result.getTotalElements());
        }
    }

    @Nested
    @DisplayName("Get Tenant Configurations Tests")
    class GetTenantConfigsTests {

        @Test
        @DisplayName("Should retrieve all tenant configurations without key filter")
        void testGetTenantConfigs_AllConfigs() {
            // Arrange
            Integer tenantId = 1;
            TenantResponseDTO tenant = TenantResponseDTO.builder().id(tenantId).stateCode("TN").build();
            List<ConfigDTO> configsList = Arrays.asList(
                    ConfigDTO.builder()
                            .configKey(TenantConfigKeyEnum.TENANT_LOGO.name())
                            .configValue("{\"value\":\"https://brand.com/logo.png\"}")
                            .build()
            );

            when(tenantCommonRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
            when(tenantCommonRepository.findConfigsByTenantId(tenantId)).thenReturn(configsList);

            // Act
            TenantConfigResponseDTO result = tenantManagementService.getTenantConfigs(tenantId, null);

            // Assert
            assertNotNull(result);
            assertEquals(tenantId, result.getTenantId());
            assertTrue(result.getConfigs().containsKey(TenantConfigKeyEnum.TENANT_LOGO));
            ConfigValueDTO configValue = result.getConfigs().get(TenantConfigKeyEnum.TENANT_LOGO);
            assertTrue(configValue instanceof SimpleConfigValueDTO);
            assertEquals("https://brand.com/logo.png", ((SimpleConfigValueDTO) configValue).getValue());
            verify(tenantCommonRepository).findById(tenantId);
        }

        @Test
        @DisplayName("Should handle null filter keys and retrieve all configurations")
        void testGetTenantConfigs_NullFilterKeys() {
            // Arrange
            Integer tenantId = 1;
            TenantResponseDTO tenant = TenantResponseDTO.builder().id(tenantId).stateCode("TN").build();
            List<ConfigDTO> configsList = Collections.singletonList(
                    ConfigDTO.builder()
                            .configKey(TenantConfigKeyEnum.TENANT_LOGO.name())
                            .configValue("{\"value\": \"https://brand.com/logo.png\"}")
                            .build()
            );

            when(tenantCommonRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
            when(tenantCommonRepository.findConfigsByTenantId(tenantId))
                    .thenReturn(configsList);

            // Act
            TenantConfigResponseDTO result = tenantManagementService.getTenantConfigs(tenantId, null);

            // Assert
            assertNotNull(result);
            assertEquals(1, result.getConfigs().size());
            assertTrue(result.getConfigs().containsKey(TenantConfigKeyEnum.TENANT_LOGO));
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when tenant not found")
        void testGetTenantConfigs_TenantNotFound() {
            // Arrange
            Integer tenantId = 999;
            when(tenantCommonRepository.findById(tenantId)).thenReturn(Optional.empty());

            // Act & Assert
            assertThrows(ResourceNotFoundException.class, 
                    () -> tenantManagementService.getTenantConfigs(tenantId, null));
        }

        @Test
        @DisplayName("Should return empty configurations when no configs exist")
        void testGetTenantConfigs_EmptyConfigs() {
            // Arrange
            Integer tenantId = 1;
            TenantResponseDTO tenant = TenantResponseDTO.builder().id(tenantId).stateCode("TN").build();

            when(tenantCommonRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
            when(tenantCommonRepository.findConfigsByTenantId(tenantId)).thenReturn(Collections.emptyList());

            // Act
            TenantConfigResponseDTO result = tenantManagementService.getTenantConfigs(tenantId, null);

            // Assert
            assertNotNull(result);
            assertEquals(tenantId, result.getTenantId());
            assertTrue(result.getConfigs().isEmpty());
        }
    }

    @Nested
    @DisplayName("Set Tenant Configurations Tests")
    class SetTenantConfigsTests {

        @Test
        @DisplayName("Should set tenant configurations successfully")
        void testSetTenantConfigs_Success() throws Exception {
            // Arrange
            Integer tenantId = 1;
            TenantResponseDTO tenant = TenantResponseDTO.builder().id(tenantId).stateCode("TN").build();
            Map<TenantConfigKeyEnum, JsonNode> newConfigs = new HashMap<>();
            // Input should be JSON that matches SimpleConfigValueDTO structure with "value" field
            newConfigs.put(TenantConfigKeyEnum.EMAIL_TEMPLATE_JSON, objectMapper.readTree("{\"value\": \"{\\\"welcome\\\": \\\"...\\\"}\"}"));

            SetTenantConfigRequestDTO request = SetTenantConfigRequestDTO.builder()
                    .configs(newConfigs)
                    .build();

            ConfigDTO savedConfig = ConfigDTO.builder()
                    .configKey(TenantConfigKeyEnum.EMAIL_TEMPLATE_JSON.name())
                    .configValue("{\"value\":\"{\\\"welcome\\\": \\\"...\\\"}\"}") 
                    .build();

            when(tenantCommonRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
            when(SecurityUtils.getCurrentUserUuid()).thenReturn("user-uuid");
            when(tenantCommonRepository.findUserIdByUuid("user-uuid")).thenReturn(Optional.of(100));
            when(tenantCommonRepository.upsertConfig(
                    eq(tenantId), 
                    eq(TenantConfigKeyEnum.EMAIL_TEMPLATE_JSON.name()),
                    anyString(),
                    eq(100)
            )).thenReturn(Optional.of(savedConfig));

            // Act
            TenantConfigResponseDTO result = tenantManagementService.setTenantConfigs(tenantId, request);

            // Assert
            assertNotNull(result);
            assertEquals(tenantId, result.getTenantId());
            ConfigValueDTO configValue = result.getConfigs().get(TenantConfigKeyEnum.EMAIL_TEMPLATE_JSON);
            assertTrue(configValue instanceof SimpleConfigValueDTO);
            assertEquals("{\"welcome\": \"...\"}", ((SimpleConfigValueDTO) configValue).getValue());
            verify(tenantCommonRepository).upsertConfig(eq(tenantId), any(), any(), any());
        }

        @Test
        @DisplayName("Should throw exception when config upsert fails")
        void testSetTenantConfigs_UpsertFailed() throws Exception {
            // Arrange
            Integer tenantId = 1;
            TenantResponseDTO tenant = TenantResponseDTO.builder().id(tenantId).build();
            Map<TenantConfigKeyEnum, JsonNode> configs = new HashMap<>();
            // Use valid JSON format that matches SimpleConfigValueDTO structure
            configs.put(TenantConfigKeyEnum.EMAIL_TEMPLATE_JSON, objectMapper.readTree("{\"value\": \"test value\"}"));

            SetTenantConfigRequestDTO request = SetTenantConfigRequestDTO.builder()
                    .configs(configs)
                    .build();

            when(tenantCommonRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
            when(SecurityUtils.getCurrentUserUuid()).thenReturn("user-uuid");
            when(tenantCommonRepository.findUserIdByUuid("user-uuid")).thenReturn(Optional.of(100));
            when(tenantCommonRepository.upsertConfig(eq(tenantId), anyString(), anyString(), eq(100)))
                    .thenReturn(Optional.empty());

            // Act & Assert
            assertThrows(RuntimeException.class, 
                    () -> tenantManagementService.setTenantConfigs(tenantId, request));
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when tenant not found")
        void testSetTenantConfigs_TenantNotFound() {
            // Arrange
            Integer tenantId = 999;
            SetTenantConfigRequestDTO request = SetTenantConfigRequestDTO.builder().build();

            when(tenantCommonRepository.findById(tenantId)).thenReturn(Optional.empty());

            // Act & Assert
            assertThrows(ResourceNotFoundException.class,
                    () -> tenantManagementService.setTenantConfigs(tenantId, request));
        }

        @Test
        @DisplayName("Should call rescheduleForTenant after persisting a GENERIC schedule config")
        void testSetTenantConfigs_TriggersReschedule() throws Exception {
            // Arrange
            Integer tenantId = 1;
            TenantResponseDTO tenant = TenantResponseDTO.builder()
                    .id(tenantId).stateCode("MP").build();
            Map<TenantConfigKeyEnum, JsonNode> configs = new HashMap<>();
            configs.put(TenantConfigKeyEnum.PUMP_OPERATOR_REMINDER_NUDGE_TIME,
                    objectMapper.readTree("{\"nudge\":{\"schedule\":{\"hour\":8,\"minute\":0}}}"));

            SetTenantConfigRequestDTO request = SetTenantConfigRequestDTO.builder()
                    .configs(configs)
                    .build();

            ConfigDTO savedConfig = ConfigDTO.builder()
                    .configKey(TenantConfigKeyEnum.PUMP_OPERATOR_REMINDER_NUDGE_TIME.name())
                    .configValue("{\"nudge\":{\"schedule\":{\"hour\":8,\"minute\":0}}}")
                    .build();

            when(tenantCommonRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
            when(SecurityUtils.getCurrentUserUuid()).thenReturn("user-uuid");
            when(tenantCommonRepository.findUserIdByUuid("user-uuid")).thenReturn(Optional.of(100));
            when(tenantCommonRepository.upsertConfig(
                    eq(tenantId),
                    eq(TenantConfigKeyEnum.PUMP_OPERATOR_REMINDER_NUDGE_TIME.name()),
                    anyString(),
                    eq(100)))
                    .thenReturn(Optional.of(savedConfig));

            // Act
            tenantManagementService.setTenantConfigs(tenantId, request);

            // Assert
            verify(schedulerManager).rescheduleForTenant(tenantId, "MP");
        }
    }

    @Nested
    @DisplayName("Get Tenant Departments Tests")
    class GetTenantDepartmentsTests {

        @Test
        @DisplayName("Should retrieve all departments for tenant schema")
        void testGetTenantDepartments_Success() {
            // Arrange
            List<DepartmentResponseDTO> departments = Arrays.asList(
                    DepartmentResponseDTO.builder().id(1).title("Health").build(),
                    DepartmentResponseDTO.builder().id(2).title("Operations").build()
            );

            // Mock TenantContext via reflection or static mock
            try (MockedStatic<TenantContext> tenantContextMock = mockStatic(TenantContext.class)) {
                tenantContextMock.when(TenantContext::getSchema).thenReturn("tenant_tn");
                when(tenantSchemaRepository.getDepartments("tenant_tn")).thenReturn(departments);

                // Act
                List<DepartmentResponseDTO> result = tenantManagementService.getTenantDepartments();

                // Assert
                assertNotNull(result);
                assertEquals(2, result.size());
                verify(tenantSchemaRepository).getDepartments("tenant_tn");
            }
        }

        @Test
        @DisplayName("Should throw exception when schema is not resolved")
        void testGetTenantDepartments_NoSchema() {
            try (MockedStatic<TenantContext> tenantContextMock = mockStatic(TenantContext.class)) {
                tenantContextMock.when(TenantContext::getSchema).thenReturn(null);

                // Act & Assert
                assertThrows(RuntimeException.class, 
                        () -> tenantManagementService.getTenantDepartments());
            }
        }
    }

    @Nested
    @DisplayName("Create Department Tests")
    class CreateDepartmentTests {

        @Test
        @DisplayName("Should create department successfully")
        void testCreateDepartment_Success() {
            // Arrange
            CreateDepartmentRequestDTO request = new CreateDepartmentRequestDTO();
            request.setTitle("Health");
            request.setStatus(1);
            request.setParentId(0);

            DepartmentResponseDTO expectedResponse = DepartmentResponseDTO.builder()
                    .id(1)
                    .title("Health")
                    .status(1)
                    .build();

            try (MockedStatic<TenantContext> tenantContextMock = mockStatic(TenantContext.class)) {
                tenantContextMock.when(TenantContext::getSchema).thenReturn("tenant_tn");
                mockedSecurityUtils.when(SecurityUtils::getCurrentUserUuid).thenReturn("user-uuid");
                when(tenantCommonRepository.findUserIdByUuid("user-uuid")).thenReturn(Optional.of(100));
                when(tenantSchemaRepository.createDepartment(eq("tenant_tn"), any(CreateDepartmentRequestDTO.class), eq(100)))
                        .thenReturn(Optional.of(expectedResponse));

                // Act
                DepartmentResponseDTO result = tenantManagementService.createDepartment(request);

                // Assert
                assertNotNull(result);
                assertEquals("Health", result.getTitle());
                assertEquals(1, result.getStatus());
                verify(tenantSchemaRepository).createDepartment(eq("tenant_tn"), any(), eq(100));
            }
        }

        @Test
        @DisplayName("Should throw exception when department creation fails")
        void testCreateDepartment_Failed() {
            // Arrange
            CreateDepartmentRequestDTO request = new CreateDepartmentRequestDTO();
            request.setTitle("Health");

            try (MockedStatic<TenantContext> tenantContextMock = mockStatic(TenantContext.class)) {
                tenantContextMock.when(TenantContext::getSchema).thenReturn("tenant_tn");
                mockedSecurityUtils.when(SecurityUtils::getCurrentUserUuid).thenReturn("user-uuid");
                when(tenantCommonRepository.findUserIdByUuid("user-uuid")).thenReturn(Optional.of(100));
                when(tenantSchemaRepository.createDepartment(eq("tenant_tn"), any(CreateDepartmentRequestDTO.class), eq(100)))
                        .thenThrow(new RuntimeException("Database error"));

                // Act & Assert
                assertThrows(RuntimeException.class, 
                        () -> tenantManagementService.createDepartment(request));
            }
        }    
    }

    @Nested
    @DisplayName("Location Hierarchy Tests")
    class LocationHierarchyTests {
        
        @Test
        @DisplayName("Should get location hierarchy successfully for LGD")
        void testGetLocationHierarchy_Success() {
            // Arrange
            Integer tenantId = 1;
            String hierarchyType = "LGD";
            
            TenantResponseDTO tenant = TenantResponseDTO.builder()
                    .id(tenantId)
                    .stateCode("mp")
                    .build();
            
            List<LocationLevelConfigDTO> levels = List.of(
                    LocationLevelConfigDTO.builder().level(1)
                            .levelName(List.of(LocationLevelNameDTO.builder().title("State").build())).build(),
                    LocationLevelConfigDTO.builder().level(2)
                            .levelName(List.of(LocationLevelNameDTO.builder().title("District").build())).build()
            );

            LocationConfigDTO locationConfig = LocationConfigDTO.builder()
                    .locationHierarchy(levels)
                    .build();
            
            when(tenantCommonRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
            when(tenantSchemaRepository.getLocationHierarchy("tenant_mp", RegionTypeEnum.LGD))
                    .thenReturn(locationConfig);
            
            // Act
            LocationHierarchyResponseDTO result = tenantManagementService.getLocationHierarchy(tenantId, hierarchyType);
            
            // Assert
            assertNotNull(result);
            assertEquals("LGD", result.getHierarchyType());
            assertEquals(2, result.getLevels().size());
            verify(tenantCommonRepository).findById(tenantId);
            verify(tenantSchemaRepository).getLocationHierarchy("tenant_mp", RegionTypeEnum.LGD);
        }
        
        @Test
        @DisplayName("Should throw exception when tenant not found")
        void testGetLocationHierarchy_TenantNotFound() {
            // Arrange
            Integer tenantId = 999;
            String hierarchyType = "LGD";
            
            when(tenantCommonRepository.findById(tenantId)).thenReturn(Optional.empty());
            
            // Act & Assert
            assertThrows(ResourceNotFoundException.class,
                    () -> tenantManagementService.getLocationHierarchy(tenantId, hierarchyType));
            verify(tenantCommonRepository).findById(tenantId);
        }
        
        @Test
        @DisplayName("Should get location children successfully for root level (parentId=null)")
        void testGetLocationChildren_RootLevel_Success() {
            // Arrange
            Integer tenantId = 1;
            String hierarchyType = "LGD";
            Integer parentId = null;
            
            TenantResponseDTO tenant = TenantResponseDTO.builder()
                    .id(tenantId)
                    .stateCode("mp")
                    .build();
            
            List<LocationResponseDTO> children = List.of(
                    LocationResponseDTO.builder().id(1).uuid("uuid-1").title("Madhya Pradesh").status(1).build()
            );
            
            when(tenantCommonRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
            when(tenantSchemaRepository.findLgdLocationsByParentId("tenant_mp", parentId))
                    .thenReturn(children);
            
            // Act
            List<LocationResponseDTO> result = tenantManagementService.getLocationChildren(tenantId, hierarchyType, parentId);
            
            // Assert
            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals("Madhya Pradesh", result.get(0).getTitle());
            verify(tenantCommonRepository).findById(tenantId);
            verify(tenantSchemaRepository).findLgdLocationsByParentId("tenant_mp", parentId);
        }
        
        @Test
        @DisplayName("Should get location children successfully for DEPARTMENT hierarchy")
        void testGetLocationChildren_Department_Success() {
            // Arrange
            Integer tenantId = 1;
            String hierarchyType = "DEPARTMENT";
            Integer parentId = 1;
            
            TenantResponseDTO tenant = TenantResponseDTO.builder()
                    .id(tenantId)
                    .stateCode("mp")
                    .build();
            
            List<LocationResponseDTO> children = List.of(
                    LocationResponseDTO.builder().id(10).uuid("uuid-10").title("Zone A").parentId(1).status(1).build(),
                    LocationResponseDTO.builder().id(11).uuid("uuid-11").title("Zone B").parentId(1).status(1).build()
            );
            
            when(tenantCommonRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
            when(tenantSchemaRepository.findDepartmentLocationsByParentId("tenant_mp", parentId))
                    .thenReturn(children);
            
            // Act
            List<LocationResponseDTO> result = tenantManagementService.getLocationChildren(tenantId, hierarchyType, parentId);
            
            // Assert
            assertNotNull(result);
            assertEquals(2, result.size());
            assertEquals("Zone A", result.get(0).getTitle());
            verify(tenantCommonRepository).findById(tenantId);
            verify(tenantSchemaRepository).findDepartmentLocationsByParentId("tenant_mp", parentId);
        }
        
        @Test
        @DisplayName("Should return empty list for children when no results found")
        void testGetLocationChildren_EmptyResult() {
            // Arrange
            Integer tenantId = 1;
            String hierarchyType = "LGD";
            Integer parentId = 999;
            
            TenantResponseDTO tenant = TenantResponseDTO.builder()
                    .id(tenantId)
                    .stateCode("mp")
                    .build();
            
            when(tenantCommonRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
            when(tenantSchemaRepository.findLgdLocationsByParentId("tenant_mp", parentId))
                    .thenReturn(Collections.emptyList());
            
            // Act
            List<LocationResponseDTO> result = tenantManagementService.getLocationChildren(tenantId, hierarchyType, parentId);
            
            // Assert
            assertNotNull(result);
            assertTrue(result.isEmpty());
            verify(tenantSchemaRepository).findLgdLocationsByParentId("tenant_mp", parentId);
        }
        
        @Test
        @DisplayName("Should throw exception when tenant not found for location children")
        void testGetLocationChildren_TenantNotFound() {
            // Arrange
            Integer tenantId = 999;
            String hierarchyType = "LGD";
            Integer parentId = 0;

            when(tenantCommonRepository.findById(tenantId)).thenReturn(Optional.empty());

            // Act & Assert
            assertThrows(ResourceNotFoundException.class,
                    () -> tenantManagementService.getLocationChildren(tenantId, hierarchyType, parentId));
            verify(tenantCommonRepository).findById(tenantId);
        }

        @Test
        @DisplayName("Should return edit constraints with structuralChangesAllowed=true when no seeded data")
        void testGetLocationHierarchyEditConstraints_NoSeededData() {
            // Arrange
            Integer tenantId = 1;
            TenantResponseDTO tenant = TenantResponseDTO.builder().id(tenantId).stateCode("mp").build();

            when(tenantCommonRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
            when(tenantSchemaRepository.countSeededLocationData("tenant_mp", RegionTypeEnum.LGD)).thenReturn(0L);

            // Act
            LocationHierarchyEditConstraintsResponseDTO result =
                    tenantManagementService.getLocationHierarchyEditConstraints(tenantId, "LGD");

            // Assert
            assertNotNull(result);
            assertEquals("LGD", result.getHierarchyType());
            assertTrue(result.isStructuralChangesAllowed());
            assertEquals(0L, result.getSeededRecordCount());
        }

        @Test
        @DisplayName("Should return edit constraints with structuralChangesAllowed=false when seeded data exists")
        void testGetLocationHierarchyEditConstraints_SeededDataExists() {
            // Arrange
            Integer tenantId = 1;
            TenantResponseDTO tenant = TenantResponseDTO.builder().id(tenantId).stateCode("mp").build();

            when(tenantCommonRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
            when(tenantSchemaRepository.countSeededLocationData("tenant_mp", RegionTypeEnum.DEPARTMENT))
                    .thenReturn(342L);

            // Act
            LocationHierarchyEditConstraintsResponseDTO result =
                    tenantManagementService.getLocationHierarchyEditConstraints(tenantId, "DEPARTMENT");

            // Assert
            assertNotNull(result);
            assertEquals("DEPARTMENT", result.getHierarchyType());
            assertFalse(result.isStructuralChangesAllowed());
            assertEquals(342L, result.getSeededRecordCount());
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when tenant not found for edit constraints")
        void testGetLocationHierarchyEditConstraints_TenantNotFound() {
            when(tenantCommonRepository.findById(999)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,
                    () -> tenantManagementService.getLocationHierarchyEditConstraints(999, "LGD"));
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException for invalid hierarchy type in edit constraints")
        void testGetLocationHierarchyEditConstraints_InvalidType() {
            Integer tenantId = 1;
            TenantResponseDTO tenant = TenantResponseDTO.builder().id(tenantId).stateCode("mp").build();
            when(tenantCommonRepository.findById(tenantId)).thenReturn(Optional.of(tenant));

            assertThrows(IllegalArgumentException.class,
                    () -> tenantManagementService.getLocationHierarchyEditConstraints(tenantId, "INVALID"));
        }

        @Test
        @DisplayName("Should do full structural update (delete+insert) when no seeded data exists")
        void testUpdateLocationHierarchy_StructuralChange_NoSeededData() {
            // Arrange
            Integer tenantId = 1;
            TenantResponseDTO tenant = TenantResponseDTO.builder().id(tenantId).stateCode("mp").build();
            List<LocationLevelConfigDTO> existingLevels = List.of(
                    LocationLevelConfigDTO.builder().level(1)
                            .levelName(List.of(LocationLevelNameDTO.builder().title("State").build())).build()
            );
            List<LocationLevelConfigDTO> newLevels = List.of(
                    LocationLevelConfigDTO.builder().level(1)
                            .levelName(List.of(LocationLevelNameDTO.builder().title("State").build())).build(),
                    LocationLevelConfigDTO.builder().level(2)
                            .levelName(List.of(LocationLevelNameDTO.builder().title("District").build())).build()
            );

            when(tenantCommonRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
            when(SecurityUtils.getCurrentUserUuid()).thenReturn("user-uuid");
            when(tenantCommonRepository.findUserIdByUuid("user-uuid")).thenReturn(Optional.of(100));
            when(tenantSchemaRepository.getLocationHierarchy("tenant_mp", RegionTypeEnum.LGD))
                    .thenReturn(LocationConfigDTO.builder().locationHierarchy(existingLevels).build());
            when(tenantSchemaRepository.countSeededLocationData("tenant_mp", RegionTypeEnum.LGD)).thenReturn(0L);

            // Act
            LocationHierarchyResponseDTO result =
                    tenantManagementService.updateLocationHierarchy(tenantId, "LGD", newLevels);

            // Assert
            assertNotNull(result);
            assertEquals("LGD", result.getHierarchyType());
            assertEquals(2, result.getLevels().size());
            verify(tenantSchemaRepository).setLocationHierarchy("tenant_mp", RegionTypeEnum.LGD, newLevels, 100);
            verify(tenantSchemaRepository, never()).updateLevelNames(any(), any(), any(), any());
        }

        @Test
        @DisplayName("Should throw LocationHierarchyStructureLockedException when structural change attempted with seeded data")
        void testUpdateLocationHierarchy_StructuralChange_SeededDataExists() {
            // Arrange
            Integer tenantId = 1;
            TenantResponseDTO tenant = TenantResponseDTO.builder().id(tenantId).stateCode("mp").build();
            List<LocationLevelConfigDTO> existingLevels = List.of(
                    LocationLevelConfigDTO.builder().level(1)
                            .levelName(List.of(LocationLevelNameDTO.builder().title("State").build())).build()
            );
            // Incoming adds a level — structural change
            List<LocationLevelConfigDTO> newLevels = List.of(
                    LocationLevelConfigDTO.builder().level(1)
                            .levelName(List.of(LocationLevelNameDTO.builder().title("State").build())).build(),
                    LocationLevelConfigDTO.builder().level(2)
                            .levelName(List.of(LocationLevelNameDTO.builder().title("District").build())).build()
            );

            when(tenantCommonRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
            when(SecurityUtils.getCurrentUserUuid()).thenReturn("user-uuid");
            when(tenantCommonRepository.findUserIdByUuid("user-uuid")).thenReturn(Optional.of(100));
            when(tenantSchemaRepository.getLocationHierarchy("tenant_mp", RegionTypeEnum.LGD))
                    .thenReturn(LocationConfigDTO.builder().locationHierarchy(existingLevels).build());
            when(tenantSchemaRepository.countSeededLocationData("tenant_mp", RegionTypeEnum.LGD)).thenReturn(1842L);

            // Act & Assert
            assertThrows(LocationHierarchyStructureLockedException.class,
                    () -> tenantManagementService.updateLocationHierarchy(tenantId, "LGD", newLevels));

            verify(tenantSchemaRepository, never()).setLocationHierarchy(any(), any(), any(), any());
            verify(tenantSchemaRepository, never()).updateLevelNames(any(), any(), any(), any());
        }

        @Test
        @DisplayName("Should do name-only update (updateLevelNames) when only level names changed with seeded data")
        void testUpdateLocationHierarchy_NameOnlyChange_SeededDataExists() {
            // Arrange
            Integer tenantId = 1;
            TenantResponseDTO tenant = TenantResponseDTO.builder().id(tenantId).stateCode("mp").build();
            List<LocationLevelConfigDTO> existingLevels = List.of(
                    LocationLevelConfigDTO.builder().level(1)
                            .levelName(List.of(LocationLevelNameDTO.builder().title("State").build())).build(),
                    LocationLevelConfigDTO.builder().level(2)
                            .levelName(List.of(LocationLevelNameDTO.builder().title("District").build())).build()
            );
            // Same level numbers (1, 2) but renamed — not a structural change
            List<LocationLevelConfigDTO> renamedLevels = List.of(
                    LocationLevelConfigDTO.builder().level(1)
                            .levelName(List.of(LocationLevelNameDTO.builder().title("Rajya").build())).build(),
                    LocationLevelConfigDTO.builder().level(2)
                            .levelName(List.of(LocationLevelNameDTO.builder().title("Zila").build())).build()
            );

            when(tenantCommonRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
            when(SecurityUtils.getCurrentUserUuid()).thenReturn("user-uuid");
            when(tenantCommonRepository.findUserIdByUuid("user-uuid")).thenReturn(Optional.of(100));
            when(tenantSchemaRepository.getLocationHierarchy("tenant_mp", RegionTypeEnum.LGD))
                    .thenReturn(LocationConfigDTO.builder().locationHierarchy(existingLevels).build());

            // Act
            LocationHierarchyResponseDTO result =
                    tenantManagementService.updateLocationHierarchy(tenantId, "LGD", renamedLevels);

            // Assert
            assertNotNull(result);
            assertEquals("LGD", result.getHierarchyType());
            verify(tenantSchemaRepository).updateLevelNames("tenant_mp", RegionTypeEnum.LGD, renamedLevels, 100);
            verify(tenantSchemaRepository, never()).setLocationHierarchy(any(), any(), any(), any());
            verify(tenantSchemaRepository, never()).countSeededLocationData(any(), any());
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when tenant not found for updateLocationHierarchy")
        void testUpdateLocationHierarchy_TenantNotFound() {
            when(tenantCommonRepository.findById(999)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,
                    () -> tenantManagementService.updateLocationHierarchy(999, "LGD",
                            List.of(LocationLevelConfigDTO.builder().level(1).levelName(List.of()).build())));
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException for invalid hierarchy type in updateLocationHierarchy")
        void testUpdateLocationHierarchy_InvalidType() {
            Integer tenantId = 1;
            TenantResponseDTO tenant = TenantResponseDTO.builder().id(tenantId).stateCode("mp").build();
            when(tenantCommonRepository.findById(tenantId)).thenReturn(Optional.of(tenant));

            assertThrows(IllegalArgumentException.class,
                    () -> tenantManagementService.updateLocationHierarchy(tenantId, "INVALID",
                            List.of(LocationLevelConfigDTO.builder().level(1).levelName(List.of()).build())));
        }
    }
}