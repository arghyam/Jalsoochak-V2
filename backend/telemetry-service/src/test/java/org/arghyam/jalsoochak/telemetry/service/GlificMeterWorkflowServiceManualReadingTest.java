package org.arghyam.jalsoochak.telemetry.service;

import org.arghyam.jalsoochak.telemetry.dto.requests.ManualReadingRequest;
import org.arghyam.jalsoochak.telemetry.dto.response.CreateReadingResponse;
import org.arghyam.jalsoochak.telemetry.repository.TenantConfigRepository;
import org.arghyam.jalsoochak.telemetry.repository.TelemetryFlowReadingDetails;
import org.arghyam.jalsoochak.telemetry.repository.TelemetryOperator;
import org.arghyam.jalsoochak.telemetry.repository.TelemetryOperatorWithSchema;
import org.arghyam.jalsoochak.telemetry.repository.TelemetryConfirmedReadingSnapshot;
import org.arghyam.jalsoochak.telemetry.repository.TelemetryTenantRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GlificMeterWorkflowServiceManualReadingTest {

    @Mock
    private GlificOperatorContextService operatorContextService;

    @Mock
    private GlificLocalizationService localizationService;

    @Mock
    private TenantConfigRepository tenantConfigRepository;

    @Mock
    private GlificMessageTemplatesService templatesService;

    @Mock
    private TelemetryTenantRepository telemetryTenantRepository;

    @Spy
    private com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();

    @InjectMocks
    private GlificMeterWorkflowService service;

    @Test
    void manualReadingUpdatesTodaysReadingAndSetsExtractedWhenZero() {
        TelemetryOperatorWithSchema operatorWithSchema = new TelemetryOperatorWithSchema(
                "tenant_test",
                new TelemetryOperator(1L, 1, "op", "op@example.com", "919999999999", null)
        );

        when(operatorContextService.resolveOperatorWithSchema("919999999999")).thenReturn(operatorWithSchema);
        when(operatorContextService.resolveOperatorLanguage(operatorWithSchema, 1)).thenReturn("en");
        when(localizationService.normalizeLanguageKey("en")).thenReturn("english");

        when(telemetryTenantRepository.findFirstSchemeForUser("tenant_test", 1L)).thenReturn(Optional.of(10L));
        when(telemetryTenantRepository.findLatestPendingMeterChangeRecord("tenant_test", 10L, 1L))
                .thenReturn(Optional.empty());
        when(telemetryTenantRepository.findLatestConfirmedReadingSnapshot("tenant_test", 10L, null))
                .thenReturn(Optional.empty());

        when(telemetryTenantRepository.findLatestFlowReadingForDate("tenant_test", 10L, 1L, LocalDate.now()))
                .thenReturn(Optional.of(new TelemetryFlowReadingDetails(
                        99L,
                        "bfm-1",
                        1L,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO
                )));

        when(telemetryTenantRepository.countAnomaliesByTypeForToday(anyString(), anyLong(), anyLong(), anyInt())).thenReturn(0);
        when(telemetryTenantRepository.findAnomalyDatesByType(anyString(), anyLong(), anyLong(), anyInt(), anyInt())).thenReturn(List.of());
        doNothing().when(telemetryTenantRepository).createAnomalyRecord(
                anyString(),
                anyInt(),
                anyLong(),
                anyLong(),
                ArgumentMatchers.<BigDecimal>any(),
                ArgumentMatchers.<BigDecimal>any(),
                ArgumentMatchers.<BigDecimal>any(),
                anyInt(),
                ArgumentMatchers.<BigDecimal>any(),
                ArgumentMatchers.<LocalDateTime>any(),
                anyInt(),
                anyString(),
                anyInt()
        );

        when(tenantConfigRepository.findManualReadingConfirmationTemplate(anyInt(), anyString())).thenReturn(Optional.empty());

        CreateReadingResponse resp = service.manualReadingMessage(ManualReadingRequest.builder()
                .contactId("919999999999")
                .manualReading("123")
                .build());

        assertNotNull(resp);
        assertEquals(true, resp.isSuccess());
        assertEquals("CONFIRMED", resp.getQualityStatus());
        assertEquals(new BigDecimal("123"), resp.getMeterReading());
        assertEquals("bfm-1", resp.getCorrelationId());

        verify(telemetryTenantRepository).updateReadingValues("tenant_test", 99L, new BigDecimal("123"), 1L);
        verify(telemetryTenantRepository, never()).updateConfirmedReading(anyString(), anyLong(), any(), anyLong());
        verify(telemetryTenantRepository, never()).createFlowReading(anyString(), anyLong(), anyLong(), any(), any(), any(), anyString(), anyString(), any());
    }

    @Test
    void manualReadingUpdatesTodaysReadingAndOnlySetsConfirmedWhenExtractedAlreadyPresent() {
        TelemetryOperatorWithSchema operatorWithSchema = new TelemetryOperatorWithSchema(
                "tenant_test",
                new TelemetryOperator(1L, 1, "op", "op@example.com", "919999999999", null)
        );

        when(operatorContextService.resolveOperatorWithSchema("919999999999")).thenReturn(operatorWithSchema);
        when(operatorContextService.resolveOperatorLanguage(operatorWithSchema, 1)).thenReturn("en");
        when(localizationService.normalizeLanguageKey("en")).thenReturn("english");

        when(telemetryTenantRepository.findFirstSchemeForUser("tenant_test", 1L)).thenReturn(Optional.of(10L));
        when(telemetryTenantRepository.findLatestPendingMeterChangeRecord("tenant_test", 10L, 1L))
                .thenReturn(Optional.empty());
        when(telemetryTenantRepository.findLatestConfirmedReadingSnapshot("tenant_test", 10L, null))
                .thenReturn(Optional.empty());

        when(telemetryTenantRepository.findLatestFlowReadingForDate("tenant_test", 10L, 1L, LocalDate.now()))
                .thenReturn(Optional.of(new TelemetryFlowReadingDetails(
                        77L,
                        "bfm-2",
                        1L,
                        new BigDecimal("111"),
                        BigDecimal.ZERO
                )));

        when(telemetryTenantRepository.countAnomaliesByTypeForToday(anyString(), anyLong(), anyLong(), anyInt())).thenReturn(0);
        when(telemetryTenantRepository.findAnomalyDatesByType(anyString(), anyLong(), anyLong(), anyInt(), anyInt())).thenReturn(List.of());
        doNothing().when(telemetryTenantRepository).createAnomalyRecord(
                anyString(),
                anyInt(),
                anyLong(),
                anyLong(),
                ArgumentMatchers.<BigDecimal>any(),
                ArgumentMatchers.<BigDecimal>any(),
                ArgumentMatchers.<BigDecimal>any(),
                anyInt(),
                ArgumentMatchers.<BigDecimal>any(),
                ArgumentMatchers.<LocalDateTime>any(),
                anyInt(),
                anyString(),
                anyInt()
        );

        when(tenantConfigRepository.findManualReadingConfirmationTemplate(anyInt(), anyString())).thenReturn(Optional.empty());

        CreateReadingResponse resp = service.manualReadingMessage(ManualReadingRequest.builder()
                .contactId("919999999999")
                .manualReading("123")
                .build());

        assertNotNull(resp);
        assertEquals(true, resp.isSuccess());
        assertEquals("CONFIRMED", resp.getQualityStatus());
        assertEquals(new BigDecimal("123"), resp.getMeterReading());
        assertEquals("bfm-2", resp.getCorrelationId());

        verify(telemetryTenantRepository).updateConfirmedReading("tenant_test", 77L, new BigDecimal("123"), 1L);
        verify(telemetryTenantRepository, never()).updateReadingValues(anyString(), anyLong(), any(), anyLong());
        verify(telemetryTenantRepository, never()).createFlowReading(anyString(), anyLong(), anyLong(), any(), any(), any(), anyString(), anyString(), any());
    }

    @Test
    void manualReadingRejectsLowerReadingWhenMeterNotReplaced() {
        TelemetryOperatorWithSchema operatorWithSchema = new TelemetryOperatorWithSchema(
                "tenant_test",
                new TelemetryOperator(1L, 1, "op", "op@example.com", "919999999999", null)
        );

        when(operatorContextService.resolveOperatorWithSchema("919999999999")).thenReturn(operatorWithSchema);
        when(operatorContextService.resolveOperatorLanguage(operatorWithSchema, 1)).thenReturn("en");
        when(localizationService.normalizeLanguageKey("en")).thenReturn("english");

        when(telemetryTenantRepository.findFirstSchemeForUser("tenant_test", 1L)).thenReturn(Optional.of(10L));
        when(telemetryTenantRepository.findLatestPendingMeterChangeRecord("tenant_test", 10L, 1L))
                .thenReturn(Optional.empty());
        when(telemetryTenantRepository.findLatestConfirmedReadingSnapshot("tenant_test", 10L, null))
                .thenReturn(Optional.of(new TelemetryConfirmedReadingSnapshot(new BigDecimal("200"), LocalDateTime.now().minusDays(1))));

        // Less-than-previous path should short-circuit before any flow-reading mutations.
        CreateReadingResponse resp = service.manualReadingMessage(ManualReadingRequest.builder()
                .contactId("919999999999")
                .manualReading("100")
                .isMeterReplaced(false)
                .build());

        assertNotNull(resp);
        assertEquals(false, resp.isSuccess());
        assertEquals("REJECTED", resp.getQualityStatus());

        verify(telemetryTenantRepository, never()).updateReadingValues(anyString(), anyLong(), any(), anyLong());
        verify(telemetryTenantRepository, never()).updateConfirmedReading(anyString(), anyLong(), any(), anyLong());
        verify(telemetryTenantRepository, never()).updateMeterChangeReason(anyString(), anyLong(), anyString(), anyLong());
        verify(telemetryTenantRepository, never()).createFlowReading(anyString(), anyLong(), anyLong(), any(), any(), any(), anyString(), anyString(), any());
    }

    @Test
    void manualReadingAcceptsLowerReadingWhenMeterReplacedAndRecordsReason() {
        TelemetryOperatorWithSchema operatorWithSchema = new TelemetryOperatorWithSchema(
                "tenant_test",
                new TelemetryOperator(1L, 1, "op", "op@example.com", "919999999999", null)
        );

        when(operatorContextService.resolveOperatorWithSchema("919999999999")).thenReturn(operatorWithSchema);
        when(operatorContextService.resolveOperatorLanguage(operatorWithSchema, 1)).thenReturn("en");
        when(localizationService.normalizeLanguageKey("en")).thenReturn("english");

        when(telemetryTenantRepository.findFirstSchemeForUser("tenant_test", 1L)).thenReturn(Optional.of(10L));
        when(telemetryTenantRepository.findLatestPendingMeterChangeRecord("tenant_test", 10L, 1L))
                .thenReturn(Optional.empty());
        when(telemetryTenantRepository.findLatestConfirmedReadingSnapshot("tenant_test", 10L, null))
                .thenReturn(Optional.of(new TelemetryConfirmedReadingSnapshot(new BigDecimal("200"), LocalDateTime.now().minusDays(1))));

        when(telemetryTenantRepository.findLatestFlowReadingForDate("tenant_test", 10L, 1L, LocalDate.now()))
                .thenReturn(Optional.of(new TelemetryFlowReadingDetails(
                        55L,
                        "bfm-55",
                        1L,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO
                )));

        when(telemetryTenantRepository.countAnomaliesByTypeForToday(anyString(), anyLong(), anyLong(), anyInt())).thenReturn(0);
        when(telemetryTenantRepository.findAnomalyDatesByType(anyString(), anyLong(), anyLong(), anyInt(), anyInt())).thenReturn(List.of());
        doNothing().when(telemetryTenantRepository).createAnomalyRecord(
                anyString(),
                anyInt(),
                anyLong(),
                anyLong(),
                ArgumentMatchers.<BigDecimal>any(),
                ArgumentMatchers.<BigDecimal>any(),
                ArgumentMatchers.<BigDecimal>any(),
                anyInt(),
                ArgumentMatchers.<BigDecimal>any(),
                ArgumentMatchers.<LocalDateTime>any(),
                anyInt(),
                anyString(),
                anyInt()
        );

        when(tenantConfigRepository.findManualReadingConfirmationTemplate(anyInt(), anyString())).thenReturn(Optional.empty());

        CreateReadingResponse resp = service.manualReadingMessage(ManualReadingRequest.builder()
                .contactId("919999999999")
                .manualReading("100")
                .isMeterReplaced(true)
                .build());

        assertNotNull(resp);
        assertEquals(true, resp.isSuccess());
        assertEquals("CONFIRMED", resp.getQualityStatus());
        assertEquals(new BigDecimal("100"), resp.getMeterReading());
        assertEquals("bfm-55", resp.getCorrelationId());

        verify(telemetryTenantRepository).updateReadingValues("tenant_test", 55L, new BigDecimal("100"), 1L);
        verify(telemetryTenantRepository).updateMeterChangeReason("tenant_test", 55L, "METER_REPLACED", 1L);
        verify(telemetryTenantRepository, never()).createFlowReading(anyString(), anyLong(), anyLong(), any(), any(), any(), anyString(), anyString(), any());
    }
}
