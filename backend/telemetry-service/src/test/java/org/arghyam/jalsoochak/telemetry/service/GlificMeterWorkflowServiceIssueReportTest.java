package org.arghyam.jalsoochak.telemetry.service;

import org.arghyam.jalsoochak.telemetry.dto.requests.IssueReportRequest;
import org.arghyam.jalsoochak.telemetry.dto.response.IntroResponse;
import org.arghyam.jalsoochak.telemetry.event.TelemetryEventPublisher;
import org.arghyam.jalsoochak.telemetry.repository.TenantConfigRepository;
import org.arghyam.jalsoochak.telemetry.repository.TelemetryOperator;
import org.arghyam.jalsoochak.telemetry.repository.TelemetryOperatorWithSchema;
import org.arghyam.jalsoochak.telemetry.repository.TelemetryTenantRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GlificMeterWorkflowServiceIssueReportTest {

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

    @Mock
    private TelemetryEventPublisher telemetryEventPublisher;

    @Spy
    private com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();

    @InjectMocks
    private GlificMeterWorkflowService service;

    @Test
    void issueReportSubmitStoresReason2AsAnomalyAndNotInFlowReading() {
        TelemetryOperatorWithSchema operatorWithSchema = new TelemetryOperatorWithSchema(
                "tenant_test",
                new TelemetryOperator(1L, 1, "op", "op@example.com", "919999999999", null)
        );

        when(operatorContextService.resolveOperatorWithSchema("919999999999")).thenReturn(operatorWithSchema);
        when(operatorContextService.resolveOperatorLanguage(operatorWithSchema, 1)).thenReturn("en");
        when(localizationService.normalizeLanguageKey("en")).thenReturn("english");

        when(templatesService.resolveScreenReasons(1, "ISSUE_REPORT")).thenReturn(List.of());
        when(tenantConfigRepository.findIssueReportReasons(1, "english")).thenReturn(List.of());
        when(templatesService.resolveScreenConfirmationTemplate(1, "ISSUE_REPORT", "english")).thenReturn(Optional.empty());
        when(tenantConfigRepository.findIssueReportConfirmationTemplate(1, "english")).thenReturn(Optional.empty());

        when(telemetryTenantRepository.findFirstSchemeForUser("tenant_test", 1L)).thenReturn(Optional.of(10L));

        IntroResponse resp = service.issueReportSubmitMessage(IssueReportRequest.builder()
                .contactId("919999999999")
                .issueReason("2")
                .build());

        assertNotNull(resp);
        assertEquals(true, resp.isSuccess());
        assertEquals("meterNotWorking", resp.getSelected());

        verify(telemetryTenantRepository).createAnomalyRecord(
                eq("tenant_test"),
                eq(AnomalyConstants.TYPE_NO_SUBMISSION),
                eq(1L),
                eq(10L),
                isNull(),
                isNull(),
                isNull(),
                eq(0),
                isNull(),
                isNull(),
                eq(0),
                eq("Meter not working"),
                eq(AnomalyConstants.STATUS_OPEN)
        );
        verify(telemetryEventPublisher).publishOutageOrNonSubmissionReason(
                eq(1),
                eq(10L),
                eq(1L),
                org.mockito.ArgumentMatchers.any(),
                eq(AnomalyConstants.TYPE_NO_SUBMISSION)
        );
        verify(telemetryTenantRepository, never()).createIssueReportRecord(
                eq("tenant_test"),
                eq(10L),
                eq(1L),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString()
        );
    }

    @Test
    void issueReportSubmitStoresReason1InFlowReadingAndNotAsAnomaly() {
        TelemetryOperatorWithSchema operatorWithSchema = new TelemetryOperatorWithSchema(
                "tenant_test",
                new TelemetryOperator(1L, 1, "op", "op@example.com", "919999999999", null)
        );

        when(operatorContextService.resolveOperatorWithSchema("919999999999")).thenReturn(operatorWithSchema);
        when(operatorContextService.resolveOperatorLanguage(operatorWithSchema, 1)).thenReturn("en");
        when(localizationService.normalizeLanguageKey("en")).thenReturn("english");

        when(templatesService.resolveScreenReasons(1, "ISSUE_REPORT")).thenReturn(List.of());
        when(tenantConfigRepository.findIssueReportReasons(1, "english")).thenReturn(List.of());
        when(templatesService.resolveScreenConfirmationTemplate(1, "ISSUE_REPORT", "english")).thenReturn(Optional.empty());
        when(tenantConfigRepository.findIssueReportConfirmationTemplate(1, "english")).thenReturn(Optional.empty());

        when(telemetryTenantRepository.findFirstSchemeForUser("tenant_test", 1L)).thenReturn(Optional.of(10L));

        IntroResponse resp = service.issueReportSubmitMessage(IssueReportRequest.builder()
                .contactId("919999999999")
                .issueReason("1")
                .build());

        assertNotNull(resp);
        assertEquals(true, resp.isSuccess());
        assertEquals("meterReplaced", resp.getSelected());

        verify(telemetryTenantRepository).createIssueReportRecord(
                eq("tenant_test"),
                eq(10L),
                eq(1L),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyString(),
                eq("Meter Replaced")
        );
        verify(telemetryTenantRepository, never()).createAnomalyRecord(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyInt(),
                org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyInt(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyInt(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyInt()
        );
    }

    @Test
    void issueReportSubmitReturnsNoWaterSuppliedForReason5() {
        TelemetryOperatorWithSchema operatorWithSchema = new TelemetryOperatorWithSchema(
                "tenant_test",
                new TelemetryOperator(1L, 1, "op", "op@example.com", "919999999999", null)
        );

        when(operatorContextService.resolveOperatorWithSchema("919999999999")).thenReturn(operatorWithSchema);
        when(operatorContextService.resolveOperatorLanguage(operatorWithSchema, 1)).thenReturn("en");
        when(localizationService.normalizeLanguageKey("en")).thenReturn("english");

        when(templatesService.resolveScreenReasons(1, "ISSUE_REPORT")).thenReturn(List.of());
        when(tenantConfigRepository.findIssueReportReasons(1, "english")).thenReturn(List.of());
        when(templatesService.resolveScreenConfirmationTemplate(1, "ISSUE_REPORT", "english")).thenReturn(Optional.empty());
        when(tenantConfigRepository.findIssueReportConfirmationTemplate(1, "english")).thenReturn(Optional.empty());

        when(telemetryTenantRepository.findFirstSchemeForUser("tenant_test", 1L)).thenReturn(Optional.of(10L));

        IntroResponse resp = service.issueReportSubmitMessage(IssueReportRequest.builder()
                .contactId("919999999999")
                .issueReason("5")
                .build());

        assertNotNull(resp);
        assertEquals(true, resp.isSuccess());
        assertEquals("noWaterSupplied", resp.getSelected());

        // Current behavior: reason "5" is treated as an anomaly selection (legacy numeric rule).
        verify(telemetryTenantRepository).createAnomalyRecord(
                eq("tenant_test"),
                eq(AnomalyConstants.TYPE_NO_WATER_SUPPLY),
                eq(1L),
                eq(10L),
                isNull(),
                isNull(),
                isNull(),
                eq(0),
                isNull(),
                isNull(),
                eq(0),
                eq("No Water Supply"),
                eq(AnomalyConstants.STATUS_OPEN)
        );
        verify(telemetryEventPublisher).publishOutageOrNonSubmissionReason(
                eq(1),
                eq(10L),
                eq(1L),
                org.mockito.ArgumentMatchers.any(),
                eq(AnomalyConstants.TYPE_NO_WATER_SUPPLY)
        );
        verify(telemetryTenantRepository, never()).createIssueReportRecord(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString()
        );
    }

    @Test
    void othersSubmittedStoresFreeTextAsAnomalyAndNotInFlowReading() {
        TelemetryOperatorWithSchema operatorWithSchema = new TelemetryOperatorWithSchema(
                "tenant_test",
                new TelemetryOperator(1L, 1, "op", "op@example.com", "919999999999", null)
        );

        when(operatorContextService.resolveOperatorWithSchema("919999999999")).thenReturn(operatorWithSchema);
        when(operatorContextService.resolveOperatorLanguage(operatorWithSchema, 1)).thenReturn("en");
        when(localizationService.normalizeLanguageKey("en")).thenReturn("english");

        when(templatesService.resolveScreenConfirmationTemplate(1, "ISSUE_REPORT", "english")).thenReturn(Optional.empty());
        when(tenantConfigRepository.findIssueReportConfirmationTemplate(1, "english")).thenReturn(Optional.empty());

        when(telemetryTenantRepository.findFirstSchemeForUser("tenant_test", 1L)).thenReturn(Optional.of(10L));

        IntroResponse resp = service.othersSubmittedMessage(IssueReportRequest.builder()
                .contactId("919999999999")
                .issueReason("pipe leakage")
                .build());

        assertNotNull(resp);
        assertEquals(true, resp.isSuccess());
        assertEquals("others", resp.getSelected());

        verify(telemetryTenantRepository).createAnomalyRecord(
                eq("tenant_test"),
                eq(AnomalyConstants.TYPE_NO_SUBMISSION),
                eq(1L),
                eq(10L),
                isNull(),
                isNull(),
                isNull(),
                eq(0),
                isNull(),
                isNull(),
                eq(0),
                eq("pipe leakage"),
                eq(AnomalyConstants.STATUS_OPEN)
        );
        verify(telemetryEventPublisher).publishOutageOrNonSubmissionReason(
                eq(1),
                eq(10L),
                eq(1L),
                org.mockito.ArgumentMatchers.any(),
                eq(AnomalyConstants.TYPE_NO_SUBMISSION)
        );
        verify(telemetryTenantRepository, never()).createIssueReportRecord(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString()
        );
    }

    @Test
    void telemetryIssueReportSubmitReturnsNoWaterSuppliedForReason4() {
        TelemetryOperatorWithSchema operatorWithSchema = new TelemetryOperatorWithSchema(
                "tenant_test",
                new TelemetryOperator(1L, 1, "op", "op@example.com", "919999999999", null)
        );

        when(operatorContextService.resolveOperatorWithSchema("919999999999")).thenReturn(operatorWithSchema);
        when(operatorContextService.resolveOperatorLanguage(operatorWithSchema, 1)).thenReturn("en");
        when(localizationService.normalizeLanguageKey("en")).thenReturn("english");
        when(tenantConfigRepository.findIssueReportConfirmationTemplate(1, "english")).thenReturn(Optional.empty());

        when(telemetryTenantRepository.findFirstSchemeForUser("tenant_test", 1L)).thenReturn(Optional.of(10L));

        IntroResponse resp = service.issueReportTelemetrySubmitMessage(IssueReportRequest.builder()
                .contactId("919999999999")
                .issueReason("4")
                .build());

        assertNotNull(resp);
        assertEquals(true, resp.isSuccess());
        assertEquals("noWaterSupplied", resp.getSelected());

        // Telemetry submit: "No Water Supply" should be tracked as an issue anomaly.
        verify(telemetryTenantRepository).createAnomalyRecord(
                eq("tenant_test"),
                eq(AnomalyConstants.TYPE_NO_WATER_SUPPLY),
                eq(1L),
                eq(10L),
                isNull(),
                isNull(),
                isNull(),
                eq(0),
                isNull(),
                isNull(),
                eq(0),
                eq("No Water Supply"),
                eq(AnomalyConstants.STATUS_OPEN)
        );
        verify(telemetryEventPublisher).publishOutageOrNonSubmissionReason(
                eq(1),
                eq(10L),
                eq(1L),
                org.mockito.ArgumentMatchers.any(),
                eq(AnomalyConstants.TYPE_NO_WATER_SUPPLY)
        );
        verify(telemetryTenantRepository, never()).createIssueReportRecord(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString()
        );
    }
}
