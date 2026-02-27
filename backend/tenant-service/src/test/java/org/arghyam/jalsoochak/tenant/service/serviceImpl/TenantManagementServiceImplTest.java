package org.arghyam.jalsoochak.tenant.service.serviceImpl;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.arghyam.jalsoochak.tenant.enums.TenantConfigKeyEnum;
import org.arghyam.jalsoochak.tenant.exception.ResourceNotFoundException;
import org.arghyam.jalsoochak.tenant.kafka.KafkaProducer;
import org.arghyam.jalsoochak.tenant.repository.TenantCommonRepository;
import org.arghyam.jalsoochak.tenant.repository.TenantSchemaRepository;
import org.arghyam.jalsoochak.tenant.util.SecurityUtils;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

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
    private KafkaProducer kafkaProducer;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private HashOperations<String, Object, Object> hashOperations;

    @Mock
    private SetOperations<String, String> setOperations;

    @InjectMocks
    private TenantManagementServiceImpl tenantManagementService;

    private MockedStatic<SecurityUtils> mockedSecurityUtils;

    @BeforeEach
    void setUp() {
        mockedSecurityUtils = mockStatic(SecurityUtils.class);
        lenient().when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        lenient().when(redisTemplate.opsForSet()).thenReturn(setOperations);
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
            when(tenantCommonRepository.createTenant(eq(request), eq(100)))
                    .thenReturn(Optional.of(expectedResponse));
            when(objectMapper.writeValueAsString(any())).thenReturn("{}");

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
            verify(kafkaProducer).sendMessage(anyString());
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
            when(tenantCommonRepository.updateTenant(eq(tenantId), eq(request), eq(100)))
                    .thenReturn(Optional.of(updated));
            when(objectMapper.writeValueAsString(any())).thenReturn("{}");

            // Act
            TenantResponseDTO result = tenantManagementService.updateTenant(tenantId, request);

            // Assert
            assertNotNull(result);
            assertEquals("ACTIVE", result.getStatus());
            verify(tenantCommonRepository).updateTenant(eq(tenantId), eq(request), eq(100));
            verify(kafkaProducer).sendMessage(anyString());
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
            when(objectMapper.writeValueAsString(any())).thenReturn("{}");

            // Act
            tenantManagementService.deactivateTenant(tenantId);

            // Assert
            verify(tenantCommonRepository).deactivateTenant(eq(tenantId), eq(100));
            verify(kafkaProducer).sendMessage(anyString());
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
                            .configKey(TenantConfigKeyEnum.TENANT_LOGO_URL.name())
                            .configValue("https://brand.com/logo.png")
                            .build(),
                    ConfigDTO.builder()
                            .configKey(TenantConfigKeyEnum.SUPPORTED_LANGUAGES.name())
                            .configValue("en,ta")
                            .build()
            );

            when(tenantCommonRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
            when(tenantCommonRepository.findConfigsByTenantId(tenantId)).thenReturn(configsList);

            // Act
            TenantConfigResponseDTO result = tenantManagementService.getTenantConfigs(tenantId, null);

            // Assert
            assertNotNull(result);
            assertEquals(tenantId, result.getTenantId());
            assertEquals(2, result.getConfigs().size());
            assertEquals("https://brand.com/logo.png", 
                    result.getConfigs().get(TenantConfigKeyEnum.TENANT_LOGO_URL));
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
                            .configKey(TenantConfigKeyEnum.TENANT_LOGO_URL.name())
                            .configValue("https://brand.com/logo.png")
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
            assertTrue(result.getConfigs().containsKey(TenantConfigKeyEnum.TENANT_LOGO_URL));
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
        void testSetTenantConfigs_Success() {
            // Arrange
            Integer tenantId = 1;
            TenantResponseDTO tenant = TenantResponseDTO.builder().id(tenantId).stateCode("TN").build();
            Map<TenantConfigKeyEnum, String> newConfigs = new HashMap<>();
            newConfigs.put(TenantConfigKeyEnum.EMAIL_TEMPLATE_JSON, "{\"welcome\": \"...\"}");

            SetTenantConfigRequestDTO request = SetTenantConfigRequestDTO.builder()
                    .configs(newConfigs)
                    .build();

            ConfigDTO savedConfig = ConfigDTO.builder()
                    .configKey(TenantConfigKeyEnum.EMAIL_TEMPLATE_JSON.name())
                    .configValue("{\"welcome\": \"...\"}")
                    .build();

            when(tenantCommonRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
            when(SecurityUtils.getCurrentUserUuid()).thenReturn("user-uuid");
            when(tenantCommonRepository.findUserIdByUuid("user-uuid")).thenReturn(Optional.of(100));
            when(tenantCommonRepository.upsertConfig(
                    eq(tenantId), 
                    eq(TenantConfigKeyEnum.EMAIL_TEMPLATE_JSON.name()),
                    eq("{\"welcome\": \"...\"}"),
                    eq(100)
            )).thenReturn(Optional.of(savedConfig));

            // Act
            TenantConfigResponseDTO result = tenantManagementService.setTenantConfigs(tenantId, request);

            // Assert
            assertNotNull(result);
            assertEquals(tenantId, result.getTenantId());
            assertEquals("{\"welcome\": \"...\"}", 
                    result.getConfigs().get(TenantConfigKeyEnum.EMAIL_TEMPLATE_JSON));
            verify(tenantCommonRepository).upsertConfig(eq(tenantId), any(), any(), any());
        }

        @Test
        @DisplayName("Should throw exception when config upsert fails")
        void testSetTenantConfigs_UpsertFailed() {
            // Arrange
            Integer tenantId = 1;
            TenantResponseDTO tenant = TenantResponseDTO.builder().id(tenantId).build();
            Map<TenantConfigKeyEnum, String> configs = new HashMap<>();
            configs.put(TenantConfigKeyEnum.EMAIL_TEMPLATE_JSON, "{\"test\":true}");

            SetTenantConfigRequestDTO request = SetTenantConfigRequestDTO.builder()
                    .configs(configs)
                    .build();

            when(tenantCommonRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
            when(SecurityUtils.getCurrentUserUuid()).thenReturn("user-uuid");
            when(tenantCommonRepository.findUserIdByUuid("user-uuid")).thenReturn(Optional.of(100));
            when(tenantCommonRepository.upsertConfig(any(), any(), any(), any()))
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
        }    }
}