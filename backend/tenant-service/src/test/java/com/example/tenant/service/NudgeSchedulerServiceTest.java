package com.example.tenant.service;

import com.example.tenant.dto.TenantResponseDTO;
import com.example.tenant.event.NudgeEvent;
import com.example.tenant.kafka.KafkaProducer;
import com.example.tenant.repository.NudgeRepository;
import com.example.tenant.repository.TenantCommonRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link NudgeSchedulerService} business logic.
 *
 * <p>Verifies that the scheduler correctly iterates tenants, skips inactive ones,
 * and publishes a {@link NudgeEvent} to Kafka for each operator who has not
 * submitted a reading today.</p>
 */
@ExtendWith(MockitoExtension.class)
class NudgeSchedulerServiceTest {

    @Mock
    private TenantCommonRepository tenantCommonRepository;

    @Mock
    private NudgeRepository nudgeRepository;

    @Mock
    private KafkaProducer kafkaProducer;

    @InjectMocks
    private NudgeSchedulerService nudgeSchedulerService;

    private TenantResponseDTO activeTenant;

    @BeforeEach
    void setUp() {
        activeTenant = TenantResponseDTO.builder()
                .id(1)
                .stateCode("MP")
                .status("ACTIVE")
                .build();
    }

    @Test
    void runNudgeJob_publishesNudgeEvent_forOperatorWithoutUploadToday() {
        when(tenantCommonRepository.findAll()).thenReturn(List.of(activeTenant));
        when(nudgeRepository.findUsersWithNoUploadToday("tenant_mp")).thenReturn(
                List.of(Map.of(
                        "phone_number", "919876543210",
                        "name", "Ramesh Kumar",
                        "scheme_id", 42,
                        "language_id", 1
                ))
        );

        nudgeSchedulerService.runNudgeJob();

        ArgumentCaptor<NudgeEvent> eventCaptor = ArgumentCaptor.forClass(NudgeEvent.class);
        verify(kafkaProducer).publishJson(eq("common-topic"), eventCaptor.capture());

        NudgeEvent event = eventCaptor.getValue();
        assertThat(event.getEventType()).isEqualTo("NUDGE");
        assertThat(event.getRecipientPhone()).isEqualTo("919876543210");
        assertThat(event.getOperatorName()).isEqualTo("Ramesh Kumar");
        assertThat(event.getSchemeId()).isEqualTo("42");
        assertThat(event.getTenantId()).isEqualTo(1);
        assertThat(event.getLanguageId()).isEqualTo(1);
    }

    @Test
    void runNudgeJob_skipsInactiveTenants() {
        TenantResponseDTO inactiveTenant = TenantResponseDTO.builder()
                .id(2).stateCode("UP").status("INACTIVE").build();
        when(tenantCommonRepository.findAll()).thenReturn(List.of(inactiveTenant));

        nudgeSchedulerService.runNudgeJob();

        verifyNoInteractions(nudgeRepository, kafkaProducer);
    }

    @Test
    void runNudgeJob_skipsOperator_withBlankPhoneNumber() {
        when(tenantCommonRepository.findAll()).thenReturn(List.of(activeTenant));
        when(nudgeRepository.findUsersWithNoUploadToday("tenant_mp")).thenReturn(
                List.of(Map.of("phone_number", "", "name", "No Phone", "scheme_id", 1, "language_id", 0))
        );

        nudgeSchedulerService.runNudgeJob();

        verifyNoInteractions(kafkaProducer);
    }

    @Test
    void runNudgeJob_skipsOperator_withNullPhoneNumber() {
        Map<String, Object> rowWithNullPhone = new java.util.HashMap<>();
        rowWithNullPhone.put("phone_number", null);
        rowWithNullPhone.put("name", "Null Phone");
        rowWithNullPhone.put("scheme_id", 1);
        rowWithNullPhone.put("language_id", 0);

        when(tenantCommonRepository.findAll()).thenReturn(List.of(activeTenant));
        when(nudgeRepository.findUsersWithNoUploadToday("tenant_mp")).thenReturn(List.of(rowWithNullPhone));

        nudgeSchedulerService.runNudgeJob();

        verifyNoInteractions(kafkaProducer);
    }

    @Test
    void runNudgeJob_publishesMultipleEvents_forMultipleOperators() {
        when(tenantCommonRepository.findAll()).thenReturn(List.of(activeTenant));
        when(nudgeRepository.findUsersWithNoUploadToday("tenant_mp")).thenReturn(List.of(
                Map.of("phone_number", "911111111111", "name", "Op A", "scheme_id", 1, "language_id", 0),
                Map.of("phone_number", "912222222222", "name", "Op B", "scheme_id", 2, "language_id", 0)
        ));

        nudgeSchedulerService.runNudgeJob();

        verify(kafkaProducer, times(2)).publishJson(eq("common-topic"), any(NudgeEvent.class));
    }

    @Test
    void runNudgeJob_continuesProcessingOtherTenants_whenOneFails() {
        TenantResponseDTO tenant1 = TenantResponseDTO.builder().id(1).stateCode("MP").status("ACTIVE").build();
        TenantResponseDTO tenant2 = TenantResponseDTO.builder().id(2).stateCode("UP").status("ACTIVE").build();

        when(tenantCommonRepository.findAll()).thenReturn(List.of(tenant1, tenant2));
        when(nudgeRepository.findUsersWithNoUploadToday("tenant_mp"))
                .thenThrow(new RuntimeException("DB error for tenant_mp"));
        when(nudgeRepository.findUsersWithNoUploadToday("tenant_up"))
                .thenReturn(List.of(
                        Map.of("phone_number", "913333333333", "name", "Op UP", "scheme_id", 5, "language_id", 0)
                ));

        // Should not throw – errors are logged per-tenant and processing continues
        nudgeSchedulerService.runNudgeJob();

        verify(kafkaProducer, times(1)).publishJson(eq("common-topic"), any(NudgeEvent.class));
    }

    @Test
    void runNudgeJob_usesDefaultLanguageId_whenLanguageIdNull() {
        Map<String, Object> rowNullLang = new java.util.HashMap<>();
        rowNullLang.put("phone_number", "919000000000");
        rowNullLang.put("name", "No Lang Op");
        rowNullLang.put("scheme_id", 99);
        rowNullLang.put("language_id", null);

        when(tenantCommonRepository.findAll()).thenReturn(List.of(activeTenant));
        when(nudgeRepository.findUsersWithNoUploadToday("tenant_mp")).thenReturn(List.of(rowNullLang));

        nudgeSchedulerService.runNudgeJob();

        ArgumentCaptor<NudgeEvent> captor = ArgumentCaptor.forClass(NudgeEvent.class);
        verify(kafkaProducer).publishJson(eq("common-topic"), captor.capture());
        assertThat(captor.getValue().getLanguageId()).isEqualTo(0);
    }

    @Test
    void runNudgeJob_usesEmptySchemeId_whenSchemeIdNull() {
        Map<String, Object> rowNullScheme = new java.util.HashMap<>();
        rowNullScheme.put("phone_number", "919001110000");
        rowNullScheme.put("name", "No Scheme Op");
        rowNullScheme.put("scheme_id", null);
        rowNullScheme.put("language_id", 0);

        when(tenantCommonRepository.findAll()).thenReturn(List.of(activeTenant));
        when(nudgeRepository.findUsersWithNoUploadToday("tenant_mp")).thenReturn(List.of(rowNullScheme));

        nudgeSchedulerService.runNudgeJob();

        ArgumentCaptor<NudgeEvent> captor = ArgumentCaptor.forClass(NudgeEvent.class);
        verify(kafkaProducer).publishJson(eq("common-topic"), captor.capture());
        assertThat(captor.getValue().getSchemeId()).isEqualTo("");
    }

    @Test
    void runNudgeJob_handlesTenantWithNullId() {
        TenantResponseDTO tenantNullId = TenantResponseDTO.builder()
                .id(null).stateCode("GJ").status("ACTIVE").build();
        when(tenantCommonRepository.findAll()).thenReturn(List.of(tenantNullId));
        when(nudgeRepository.findUsersWithNoUploadToday("tenant_gj")).thenReturn(
                List.of(Map.of("phone_number", "919002220000", "name", "Op GJ", "scheme_id", 7, "language_id", 0))
        );

        nudgeSchedulerService.runNudgeJob();

        ArgumentCaptor<NudgeEvent> captor = ArgumentCaptor.forClass(NudgeEvent.class);
        verify(kafkaProducer).publishJson(eq("common-topic"), captor.capture());
        assertThat(captor.getValue().getTenantId()).isEqualTo(0); // default when id is null
    }
}
