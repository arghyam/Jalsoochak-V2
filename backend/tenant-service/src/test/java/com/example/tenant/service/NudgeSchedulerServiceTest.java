package com.example.tenant.service;

import com.example.tenant.event.NudgeEvent;
import com.example.tenant.kafka.KafkaProducer;
import com.example.tenant.repository.NudgeRepository;
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
 * Unit tests for {@link NudgeSchedulerService} business logic.
 *
 * <p>Each test calls {@code processNudgesForTenant} directly; tenant iteration
 * is handled by {@link TenantSchedulerManager} and is not exercised here.</p>
 */
@ExtendWith(MockitoExtension.class)
class NudgeSchedulerServiceTest {

    @Mock
    private NudgeRepository nudgeRepository;

    @Mock
    private KafkaProducer kafkaProducer;

    @InjectMocks
    private NudgeSchedulerService nudgeSchedulerService;

    private static final String SCHEMA = "tenant_mp";
    private static final int TENANT_ID = 1;

    @Test
    void processNudgesForTenant_publishesNudgeEvent_forOperatorWithoutUploadToday() {
        when(nudgeRepository.findUsersWithNoUploadToday(SCHEMA)).thenReturn(
                List.of(Map.of(
                        "phone_number", "919876543210",
                        "name", "Ramesh Kumar",
                        "scheme_id", 42,
                        "language_id", 1
                ))
        );

        nudgeSchedulerService.processNudgesForTenant(SCHEMA, TENANT_ID);

        ArgumentCaptor<NudgeEvent> eventCaptor = ArgumentCaptor.forClass(NudgeEvent.class);
        verify(kafkaProducer).publishJson(eq("common-topic"), eventCaptor.capture());

        NudgeEvent event = eventCaptor.getValue();
        assertThat(event.getEventType()).isEqualTo("NUDGE");
        assertThat(event.getRecipientPhone()).isEqualTo("919876543210");
        assertThat(event.getOperatorName()).isEqualTo("Ramesh Kumar");
        assertThat(event.getSchemeId()).isEqualTo("42");
        assertThat(event.getTenantId()).isEqualTo(TENANT_ID);
        assertThat(event.getLanguageId()).isEqualTo(1);
    }

    @Test
    void processNudgesForTenant_skipsOperator_withBlankPhoneNumber() {
        when(nudgeRepository.findUsersWithNoUploadToday(SCHEMA)).thenReturn(
                List.of(Map.of("phone_number", "", "name", "No Phone", "scheme_id", 1, "language_id", 0))
        );

        nudgeSchedulerService.processNudgesForTenant(SCHEMA, TENANT_ID);

        verifyNoInteractions(kafkaProducer);
    }

    @Test
    void processNudgesForTenant_skipsOperator_withNullPhoneNumber() {
        Map<String, Object> rowWithNullPhone = new HashMap<>();
        rowWithNullPhone.put("phone_number", null);
        rowWithNullPhone.put("name", "Null Phone");
        rowWithNullPhone.put("scheme_id", 1);
        rowWithNullPhone.put("language_id", 0);

        when(nudgeRepository.findUsersWithNoUploadToday(SCHEMA)).thenReturn(List.of(rowWithNullPhone));

        nudgeSchedulerService.processNudgesForTenant(SCHEMA, TENANT_ID);

        verifyNoInteractions(kafkaProducer);
    }

    @Test
    void processNudgesForTenant_publishesMultipleEvents_forMultipleOperators() {
        when(nudgeRepository.findUsersWithNoUploadToday(SCHEMA)).thenReturn(List.of(
                Map.of("phone_number", "911111111111", "name", "Op A", "scheme_id", 1, "language_id", 0),
                Map.of("phone_number", "912222222222", "name", "Op B", "scheme_id", 2, "language_id", 0)
        ));

        nudgeSchedulerService.processNudgesForTenant(SCHEMA, TENANT_ID);

        verify(kafkaProducer, times(2)).publishJson(eq("common-topic"), any(NudgeEvent.class));
    }

    @Test
    void processNudgesForTenant_usesDefaultLanguageId_whenLanguageIdNull() {
        Map<String, Object> rowNullLang = new HashMap<>();
        rowNullLang.put("phone_number", "919000000000");
        rowNullLang.put("name", "No Lang Op");
        rowNullLang.put("scheme_id", 99);
        rowNullLang.put("language_id", null);

        when(nudgeRepository.findUsersWithNoUploadToday(SCHEMA)).thenReturn(List.of(rowNullLang));

        nudgeSchedulerService.processNudgesForTenant(SCHEMA, TENANT_ID);

        ArgumentCaptor<NudgeEvent> captor = ArgumentCaptor.forClass(NudgeEvent.class);
        verify(kafkaProducer).publishJson(eq("common-topic"), captor.capture());
        assertThat(captor.getValue().getLanguageId()).isEqualTo(0);
    }

    @Test
    void processNudgesForTenant_usesEmptySchemeId_whenSchemeIdNull() {
        Map<String, Object> rowNullScheme = new HashMap<>();
        rowNullScheme.put("phone_number", "919001110000");
        rowNullScheme.put("name", "No Scheme Op");
        rowNullScheme.put("scheme_id", null);
        rowNullScheme.put("language_id", 0);

        when(nudgeRepository.findUsersWithNoUploadToday(SCHEMA)).thenReturn(List.of(rowNullScheme));

        nudgeSchedulerService.processNudgesForTenant(SCHEMA, TENANT_ID);

        ArgumentCaptor<NudgeEvent> captor = ArgumentCaptor.forClass(NudgeEvent.class);
        verify(kafkaProducer).publishJson(eq("common-topic"), captor.capture());
        assertThat(captor.getValue().getSchemeId()).isEqualTo("");
    }
}
