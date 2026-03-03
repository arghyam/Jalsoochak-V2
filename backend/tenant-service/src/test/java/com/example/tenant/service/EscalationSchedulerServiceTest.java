package com.example.tenant.service;

import com.example.tenant.dto.TenantResponseDTO;
import com.example.tenant.event.EscalationEvent;
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
 * <p>Covers:
 * <ul>
 *   <li>Level-1 (section officer) vs level-2 (district officer) routing</li>
 *   <li>Never-uploaded operators always escalated to level 2</li>
 *   <li>Batching – one {@link EscalationEvent} per officer per level</li>
 *   <li>Inactive tenant skipping and per-tenant error isolation</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class EscalationSchedulerServiceTest {

    @Mock
    private TenantCommonRepository tenantCommonRepository;

    @Mock
    private NudgeRepository nudgeRepository;

    @Mock
    private TenantConfigService tenantConfigService;

    @Mock
    private KafkaProducer kafkaProducer;

    @InjectMocks
    private EscalationSchedulerService escalationSchedulerService;

    private TenantResponseDTO activeTenant;

    @BeforeEach
    void setUp() {
        activeTenant = TenantResponseDTO.builder()
                .id(1).stateCode("MP").status("ACTIVE").build();

        when(tenantConfigService.getLevel1ThresholdDays()).thenReturn(3);
        when(tenantConfigService.getLevel2ThresholdDays()).thenReturn(7);
        when(tenantConfigService.getLevel1OfficerUserType()).thenReturn("SECTION_OFFICER");
        when(tenantConfigService.getLevel2OfficerUserType()).thenReturn("DISTRICT_OFFICER");
    }

    @Test
    void runEscalationJob_publishesLevel1Event_forOperatorMeetingLevel1Threshold() {
        when(tenantCommonRepository.findAll()).thenReturn(List.of(activeTenant));

        Map<String, Object> operatorRow = operatorRow("Op Level1", "911001001001", 1, 5);
        when(nudgeRepository.findUsersWithMissedDays("tenant_mp", 3)).thenReturn(List.of(operatorRow));

        Map<String, Object> soRow = officerRow("SO Singh", "919001001001", 1);
        when(nudgeRepository.findOfficerByUserType("tenant_mp", 1, "SECTION_OFFICER")).thenReturn(soRow);

        escalationSchedulerService.runEscalationJob();

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
    void runEscalationJob_publishesLevel2Event_forOperatorExceedingLevel2Threshold() {
        when(tenantCommonRepository.findAll()).thenReturn(List.of(activeTenant));

        Map<String, Object> operatorRow = operatorRow("Op Level2", "911002002002", 1, 8);
        when(nudgeRepository.findUsersWithMissedDays("tenant_mp", 3)).thenReturn(List.of(operatorRow));

        Map<String, Object> doRow = officerRow("DO Kumar", "919002002002", 0);
        when(nudgeRepository.findOfficerByUserType("tenant_mp", 1, "DISTRICT_OFFICER")).thenReturn(doRow);
        when(nudgeRepository.findOfficerByUserType("tenant_mp", 1, "SECTION_OFFICER"))
                .thenReturn(officerRow("SO Ref", "910000000001", 0));

        escalationSchedulerService.runEscalationJob();

        ArgumentCaptor<EscalationEvent> captor = ArgumentCaptor.forClass(EscalationEvent.class);
        verify(kafkaProducer).publishJson(eq("common-topic"), captor.capture());

        EscalationEvent event = captor.getValue();
        assertThat(event.getEscalationLevel()).isEqualTo(2);
        assertThat(event.getOfficerPhone()).isEqualTo("919002002002");
    }

    @Test
    void runEscalationJob_escalatesNeverUploadedOperator_toLevel2() {
        when(tenantCommonRepository.findAll()).thenReturn(List.of(activeTenant));

        // null days_since_last_upload = never uploaded → always level 2
        Map<String, Object> neverUploaded = new HashMap<>();
        neverUploaded.put("name", "Op Never");
        neverUploaded.put("phone_number", "911003003003");
        neverUploaded.put("scheme_id", 1);
        neverUploaded.put("scheme_name", "Scheme A");
        neverUploaded.put("language_id", 0);
        neverUploaded.put("last_reading_date", null);
        neverUploaded.put("days_since_last_upload", null); // never uploaded

        when(nudgeRepository.findUsersWithMissedDays("tenant_mp", 3)).thenReturn(List.of(neverUploaded));

        Map<String, Object> doRow = officerRow("DO Patel", "919003003003", 0);
        when(nudgeRepository.findOfficerByUserType("tenant_mp", 1, "DISTRICT_OFFICER")).thenReturn(doRow);
        when(nudgeRepository.findOfficerByUserType("tenant_mp", 1, "SECTION_OFFICER"))
                .thenReturn(officerRow("SO Ref", "910000000002", 0));

        escalationSchedulerService.runEscalationJob();

        ArgumentCaptor<EscalationEvent> captor = ArgumentCaptor.forClass(EscalationEvent.class);
        verify(kafkaProducer).publishJson(eq("common-topic"), captor.capture());

        assertThat(captor.getValue().getEscalationLevel()).isEqualTo(2);
        assertThat(captor.getValue().getOperators().get(0).getLastRecordedBfmDate()).isEqualTo("Never");
    }

    @Test
    void runEscalationJob_batchesMultipleOperators_underSameOfficer() {
        when(tenantCommonRepository.findAll()).thenReturn(List.of(activeTenant));

        Map<String, Object> op1 = operatorRow("Op A", "911111111110", 1, 4);
        Map<String, Object> op2 = operatorRow("Op B", "912222222220", 1, 5);
        when(nudgeRepository.findUsersWithMissedDays("tenant_mp", 3)).thenReturn(List.of(op1, op2));

        Map<String, Object> soRow = officerRow("SO Batch", "919009009009", 0);
        when(nudgeRepository.findOfficerByUserType(eq("tenant_mp"), eq(1), eq("SECTION_OFFICER")))
                .thenReturn(soRow);

        escalationSchedulerService.runEscalationJob();

        // Both operators grouped under the same section officer → one event
        ArgumentCaptor<EscalationEvent> captor = ArgumentCaptor.forClass(EscalationEvent.class);
        verify(kafkaProducer, times(1)).publishJson(eq("common-topic"), captor.capture());

        assertThat(captor.getValue().getOperators()).hasSize(2);
    }

    @Test
    void runEscalationJob_publishesSeparateEvents_forLevel1AndLevel2Officers() {
        when(tenantCommonRepository.findAll()).thenReturn(List.of(activeTenant));

        Map<String, Object> l1Op = operatorRow("Op L1", "911000001111", 1, 4);
        Map<String, Object> l2Op = operatorRow("Op L2", "912000001111", 1, 8);
        when(nudgeRepository.findUsersWithMissedDays("tenant_mp", 3)).thenReturn(List.of(l1Op, l2Op));

        Map<String, Object> soRow = officerRow("SO X", "919100000001", 0);
        Map<String, Object> doRow = officerRow("DO Y", "919200000001", 0);

        // Level-1 operator → SO lookup
        when(nudgeRepository.findOfficerByUserType("tenant_mp", 1, "SECTION_OFFICER")).thenReturn(soRow);
        // Level-2 operator → DO lookup; SO lookup for soName reference
        when(nudgeRepository.findOfficerByUserType("tenant_mp", 1, "DISTRICT_OFFICER")).thenReturn(doRow);

        escalationSchedulerService.runEscalationJob();

        verify(kafkaProducer, times(2)).publishJson(eq("common-topic"), any(EscalationEvent.class));
    }

    @Test
    void runEscalationJob_skipsOperator_whenNoOfficerFound() {
        when(tenantCommonRepository.findAll()).thenReturn(List.of(activeTenant));
        when(nudgeRepository.findUsersWithMissedDays("tenant_mp", 3))
                .thenReturn(List.of(operatorRow("Op Orphan", "913000000001", 1, 4)));
        when(nudgeRepository.findOfficerByUserType(any(), any(), any())).thenReturn(null);

        escalationSchedulerService.runEscalationJob();

        verifyNoInteractions(kafkaProducer);
    }

    @Test
    void runEscalationJob_skipsInactiveTenants() {
        TenantResponseDTO inactive = TenantResponseDTO.builder()
                .id(2).stateCode("RJ").status("INACTIVE").build();
        when(tenantCommonRepository.findAll()).thenReturn(List.of(inactive));

        escalationSchedulerService.runEscalationJob();

        verifyNoInteractions(nudgeRepository, kafkaProducer);
    }

    @Test
    void runEscalationJob_continuesForOtherTenants_whenOneFails() {
        TenantResponseDTO t1 = TenantResponseDTO.builder().id(1).stateCode("MP").status("ACTIVE").build();
        TenantResponseDTO t2 = TenantResponseDTO.builder().id(2).stateCode("UP").status("ACTIVE").build();
        when(tenantCommonRepository.findAll()).thenReturn(List.of(t1, t2));

        when(nudgeRepository.findUsersWithMissedDays("tenant_mp", 3))
                .thenThrow(new RuntimeException("DB error"));
        when(nudgeRepository.findUsersWithMissedDays("tenant_up", 3))
                .thenReturn(List.of(operatorRow("Op UP", "914000000001", 2, 4)));
        when(nudgeRepository.findOfficerByUserType(eq("tenant_up"), eq(2), eq("SECTION_OFFICER")))
                .thenReturn(officerRow("SO UP", "919400000001", 0));

        escalationSchedulerService.runEscalationJob();

        verify(kafkaProducer, times(1)).publishJson(eq("common-topic"), any(EscalationEvent.class));
    }

    @Test
    void runEscalationJob_includesOfficerLanguageId_inEvent() {
        when(tenantCommonRepository.findAll()).thenReturn(List.of(activeTenant));
        when(nudgeRepository.findUsersWithMissedDays("tenant_mp", 3))
                .thenReturn(List.of(operatorRow("Op Hindi", "915500005555", 1, 4)));

        Map<String, Object> soRow = new HashMap<>();
        soRow.put("name", "SO Hindi");
        soRow.put("phone_number", "919550005555");
        soRow.put("language_id", 2); // Hindi
        when(nudgeRepository.findOfficerByUserType("tenant_mp", 1, "SECTION_OFFICER")).thenReturn(soRow);

        escalationSchedulerService.runEscalationJob();

        ArgumentCaptor<EscalationEvent> captor = ArgumentCaptor.forClass(EscalationEvent.class);
        verify(kafkaProducer).publishJson(eq("common-topic"), captor.capture());
        assertThat(captor.getValue().getOfficerLanguageId()).isEqualTo(2);
    }

    // ────────────────────────────── helpers ────────────────────────────────────

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
