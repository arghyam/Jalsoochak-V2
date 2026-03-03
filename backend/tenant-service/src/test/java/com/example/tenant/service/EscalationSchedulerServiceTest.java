package com.example.tenant.service;

import com.example.tenant.config.EscalationScheduleConfig;
import com.example.tenant.event.EscalationEvent;
import com.example.tenant.kafka.KafkaProducer;
import com.example.tenant.repository.NudgeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link EscalationSchedulerService} business logic.
 *
 * <p>Each test calls {@code processEscalationsForTenant} directly; tenant iteration
 * is handled by {@link TenantSchedulerManager} and is not exercised here.</p>
 */
@ExtendWith(MockitoExtension.class)
class EscalationSchedulerServiceTest {

    @Mock
    private NudgeRepository nudgeRepository;

    @Mock
    private TenantConfigService tenantConfigService;

    @Mock
    private KafkaProducer kafkaProducer;

    @InjectMocks
    private EscalationSchedulerService escalationSchedulerService;

    private static final String SCHEMA = "tenant_mp";
    private static final int TENANT_ID = 1;

    @BeforeEach
    void setUp() {
        when(tenantConfigService.getEscalationConfig(TENANT_ID))
                .thenReturn(defaultConfig());
    }

    @Test
    void processEscalationsForTenant_publishesLevel1Event_forOperatorMeetingLevel1Threshold() {
        Map<String, Object> operatorRow = operatorRow("Op Level1", "911001001001", 1, 5);
        when(nudgeRepository.findUsersWithMissedDays(SCHEMA, 3)).thenReturn(List.of(operatorRow));

        Map<String, Object> soRow = officerRow("SO Singh", "919001001001", 1);
        when(nudgeRepository.findOfficerByUserType(SCHEMA, 1, "SECTION_OFFICER")).thenReturn(soRow);

        escalationSchedulerService.processEscalationsForTenant(SCHEMA, TENANT_ID);

        ArgumentCaptor<EscalationEvent> captor = ArgumentCaptor.forClass(EscalationEvent.class);
        verify(kafkaProducer).publishJson(eq("common-topic"), captor.capture());

        EscalationEvent event = captor.getValue();
        assertThat(event.getEventType()).isEqualTo("ESCALATION");
        assertThat(event.getEscalationLevel()).isEqualTo(1);
        assertThat(event.getOfficerPhone()).isEqualTo("919001001001");
        assertThat(event.getOperators()).hasSize(1);
        assertThat(event.getOperators().get(0).getName()).isEqualTo("Op Level1");
    }

    @Test
    void processEscalationsForTenant_publishesLevel2Event_forOperatorExceedingLevel2Threshold() {
        Map<String, Object> operatorRow = operatorRow("Op Level2", "911002002002", 1, 8);
        when(nudgeRepository.findUsersWithMissedDays(SCHEMA, 3)).thenReturn(List.of(operatorRow));

        Map<String, Object> doRow = officerRow("DO Kumar", "919002002002", 0);
        when(nudgeRepository.findOfficerByUserType(SCHEMA, 1, "DISTRICT_OFFICER")).thenReturn(doRow);
        when(nudgeRepository.findOfficerByUserType(SCHEMA, 1, "SECTION_OFFICER"))
                .thenReturn(officerRow("SO Ref", "910000000001", 0));

        escalationSchedulerService.processEscalationsForTenant(SCHEMA, TENANT_ID);

        ArgumentCaptor<EscalationEvent> captor = ArgumentCaptor.forClass(EscalationEvent.class);
        verify(kafkaProducer).publishJson(eq("common-topic"), captor.capture());

        assertThat(captor.getValue().getEscalationLevel()).isEqualTo(2);
        assertThat(captor.getValue().getOfficerPhone()).isEqualTo("919002002002");
    }

    @Test
    void processEscalationsForTenant_escalatesNeverUploadedOperator_toLevel2() {
        Map<String, Object> neverUploaded = new HashMap<>();
        neverUploaded.put("name", "Op Never");
        neverUploaded.put("phone_number", "911003003003");
        neverUploaded.put("scheme_id", 1);
        neverUploaded.put("scheme_name", "Scheme A");
        neverUploaded.put("language_id", 0);
        neverUploaded.put("last_reading_date", null);
        neverUploaded.put("days_since_last_upload", null);

        when(nudgeRepository.findUsersWithMissedDays(SCHEMA, 3)).thenReturn(List.of(neverUploaded));
        when(nudgeRepository.findOfficerByUserType(SCHEMA, 1, "DISTRICT_OFFICER"))
                .thenReturn(officerRow("DO Patel", "919003003003", 0));
        when(nudgeRepository.findOfficerByUserType(SCHEMA, 1, "SECTION_OFFICER"))
                .thenReturn(officerRow("SO Ref", "910000000002", 0));

        escalationSchedulerService.processEscalationsForTenant(SCHEMA, TENANT_ID);

        ArgumentCaptor<EscalationEvent> captor = ArgumentCaptor.forClass(EscalationEvent.class);
        verify(kafkaProducer).publishJson(eq("common-topic"), captor.capture());

        assertThat(captor.getValue().getEscalationLevel()).isEqualTo(2);
        assertThat(captor.getValue().getOperators().get(0).getLastRecordedBfmDate()).isEqualTo("Never");
    }

    @Test
    void processEscalationsForTenant_batchesMultipleOperators_underSameOfficer() {
        Map<String, Object> op1 = operatorRow("Op A", "911111111110", 1, 4);
        Map<String, Object> op2 = operatorRow("Op B", "912222222220", 1, 5);
        when(nudgeRepository.findUsersWithMissedDays(SCHEMA, 3)).thenReturn(List.of(op1, op2));

        Map<String, Object> soRow = officerRow("SO Batch", "919009009009", 0);
        when(nudgeRepository.findOfficerByUserType(eq(SCHEMA), eq(1), eq("SECTION_OFFICER")))
                .thenReturn(soRow);

        escalationSchedulerService.processEscalationsForTenant(SCHEMA, TENANT_ID);

        ArgumentCaptor<EscalationEvent> captor = ArgumentCaptor.forClass(EscalationEvent.class);
        verify(kafkaProducer, times(1)).publishJson(eq("common-topic"), captor.capture());
        assertThat(captor.getValue().getOperators()).hasSize(2);
    }

    @Test
    void processEscalationsForTenant_publishesSeparateEvents_forLevel1AndLevel2Officers() {
        Map<String, Object> l1Op = operatorRow("Op L1", "911000001111", 1, 4);
        Map<String, Object> l2Op = operatorRow("Op L2", "912000001111", 1, 8);
        when(nudgeRepository.findUsersWithMissedDays(SCHEMA, 3)).thenReturn(List.of(l1Op, l2Op));

        when(nudgeRepository.findOfficerByUserType(SCHEMA, 1, "SECTION_OFFICER"))
                .thenReturn(officerRow("SO X", "919100000001", 0));
        when(nudgeRepository.findOfficerByUserType(SCHEMA, 1, "DISTRICT_OFFICER"))
                .thenReturn(officerRow("DO Y", "919200000001", 0));

        escalationSchedulerService.processEscalationsForTenant(SCHEMA, TENANT_ID);

        verify(kafkaProducer, times(2)).publishJson(eq("common-topic"), any(EscalationEvent.class));
    }

    @Test
    void processEscalationsForTenant_skipsOperator_whenNoOfficerFound() {
        when(nudgeRepository.findUsersWithMissedDays(SCHEMA, 3))
                .thenReturn(List.of(operatorRow("Op Orphan", "913000000001", 1, 4)));
        when(nudgeRepository.findOfficerByUserType(any(), any(), any())).thenReturn(null);

        escalationSchedulerService.processEscalationsForTenant(SCHEMA, TENANT_ID);

        verifyNoInteractions(kafkaProducer);
    }

    @Test
    void processEscalationsForTenant_includesOfficerLanguageId_inEvent() {
        when(nudgeRepository.findUsersWithMissedDays(SCHEMA, 3))
                .thenReturn(List.of(operatorRow("Op Hindi", "915500005555", 1, 4)));

        Map<String, Object> soRow = new HashMap<>();
        soRow.put("name", "SO Hindi");
        soRow.put("phone_number", "919550005555");
        soRow.put("language_id", 2);
        when(nudgeRepository.findOfficerByUserType(SCHEMA, 1, "SECTION_OFFICER")).thenReturn(soRow);

        escalationSchedulerService.processEscalationsForTenant(SCHEMA, TENANT_ID);

        ArgumentCaptor<EscalationEvent> captor = ArgumentCaptor.forClass(EscalationEvent.class);
        verify(kafkaProducer).publishJson(eq("common-topic"), captor.capture());
        assertThat(captor.getValue().getOfficerLanguageId()).isEqualTo(2);
    }

    @Test
    void processEscalationsForTenant_fetchesConfigWithCorrectTenantId() {
        when(nudgeRepository.findUsersWithMissedDays(SCHEMA, 3)).thenReturn(List.of());

        escalationSchedulerService.processEscalationsForTenant(SCHEMA, TENANT_ID);

        verify(tenantConfigService).getEscalationConfig(TENANT_ID);
    }

    @Test
    void processEscalationsForTenant_differentTenants_useTheirOwnConfig() {
        int tenantId2 = 2;
        EscalationScheduleConfig cfg2 = EscalationScheduleConfig.builder()
                .hour(9).minute(0).level1Days(5).level1OfficerType("SECTION_OFFICER")
                .level2Days(12).level2OfficerType("DISTRICT_OFFICER").build();
        when(tenantConfigService.getEscalationConfig(tenantId2)).thenReturn(cfg2);

        when(nudgeRepository.findUsersWithMissedDays("tenant_up", 5)).thenReturn(List.of());
        when(nudgeRepository.findUsersWithMissedDays(SCHEMA, 3)).thenReturn(List.of());

        escalationSchedulerService.processEscalationsForTenant(SCHEMA, TENANT_ID);
        escalationSchedulerService.processEscalationsForTenant("tenant_up", tenantId2);

        verify(nudgeRepository).findUsersWithMissedDays(SCHEMA, 3);      // tenant 1 level1Days=3
        verify(nudgeRepository).findUsersWithMissedDays("tenant_up", 5); // tenant 2 level1Days=5
    }

    // ── helpers ─────────────────────────────────────────────────────────────────

    private EscalationScheduleConfig defaultConfig() {
        return EscalationScheduleConfig.builder()
                .hour(9).minute(0)
                .level1Days(3).level1OfficerType("SECTION_OFFICER")
                .level2Days(7).level2OfficerType("DISTRICT_OFFICER")
                .build();
    }

    private Map<String, Object> operatorRow(String name, String phone, int schemeId, int daysSince) {
        Map<String, Object> row = new HashMap<>();
        row.put("name", name);
        row.put("phone_number", phone);
        row.put("scheme_id", schemeId);
        row.put("scheme_name", "Scheme-" + schemeId);
        row.put("language_id", 0);
        row.put("last_reading_date", java.time.LocalDate.now().minusDays(daysSince).toString());
        row.put("days_since_last_upload", daysSince);
        return row;
    }

    private Map<String, Object> officerRow(String name, String phone, int languageId) {
        Map<String, Object> row = new HashMap<>();
        row.put("name", name);
        row.put("phone_number", phone);
        row.put("language_id", languageId);
        return row;
    }
}
