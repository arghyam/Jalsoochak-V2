package org.arghyam.jalsoochak.telemetry.service;

import org.arghyam.jalsoochak.telemetry.dto.requests.CreateReadingRequest;
import org.arghyam.jalsoochak.telemetry.dto.response.CreateReadingResponse;
import org.arghyam.jalsoochak.telemetry.repository.TelemetryConfirmedReadingSnapshot;
import org.arghyam.jalsoochak.telemetry.repository.TelemetryOperator;
import org.arghyam.jalsoochak.telemetry.repository.TelemetryTenantRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BfmReadingServiceMeterReplacedTest {

    @Mock
    private TelemetryTenantRepository telemetryTenantRepository;

    @Mock
    private FlowVisionService flowVisionService;

    @InjectMocks
    private BfmReadingService service;

    @Test
    void createReadingRejectsLowerReadingWhenMeterNotReplaced() {
        String schemaName = "tenant_test";
        TelemetryOperator operator = new TelemetryOperator(1L, 1, "op", "op@example.com", "919999999999", null);

        CreateReadingRequest request = CreateReadingRequest.builder()
                .schemeId(10L)
                .operatorId(1L)
                .readingValue(new BigDecimal("100"))
                .build();

        when(telemetryTenantRepository.existsSchemeById(schemaName, 10L)).thenReturn(true);
        when(telemetryTenantRepository.findOperatorById(schemaName, 1L)).thenReturn(Optional.of(operator));
        when(telemetryTenantRepository.isOperatorMappedToScheme(schemaName, 1L, 10L)).thenReturn(true);

        when(telemetryTenantRepository.findLatestConfirmedReadingSnapshot(schemaName, 10L, null))
                .thenReturn(Optional.of(new TelemetryConfirmedReadingSnapshot(new BigDecimal("200"), LocalDateTime.now().minusDays(1))));
        when(telemetryTenantRepository.findLatestConfirmedReadingSnapshotForDate(schemaName, 10L, LocalDate.now().minusDays(1), null))
                .thenReturn(Optional.of(new TelemetryConfirmedReadingSnapshot(new BigDecimal("200"), LocalDateTime.now().minusDays(1))));

        CreateReadingResponse resp = service.createReading(request, schemaName, operator, "919999999999", false);

        assertNotNull(resp);
        assertEquals(false, resp.isSuccess());
        assertEquals("REJECTED", resp.getQualityStatus());
        assertTrue(resp.getMessage().contains("Reading cannot be less than previous reading"));

        verify(telemetryTenantRepository, never()).createFlowReading(
                anyString(), anyLong(), anyLong(), any(), any(), any(), anyString(), anyString(), any()
        );
    }

    @Test
    void createReadingAcceptsLowerReadingWhenMeterReplacedAndRecordsReason() {
        String schemaName = "tenant_test";
        TelemetryOperator operator = new TelemetryOperator(1L, 1, "op", "op@example.com", "919999999999", null);

        LocalDateTime readingAt = LocalDateTime.now();
        CreateReadingRequest request = CreateReadingRequest.builder()
                .schemeId(10L)
                .operatorId(1L)
                .readingValue(new BigDecimal("100"))
                .meterChangeReason("METER_REPLACED")
                .readingTime(readingAt)
                .build();

        when(telemetryTenantRepository.existsSchemeById(schemaName, 10L)).thenReturn(true);
        when(telemetryTenantRepository.findOperatorById(schemaName, 1L)).thenReturn(Optional.of(operator));
        when(telemetryTenantRepository.isOperatorMappedToScheme(schemaName, 1L, 10L)).thenReturn(true);

        when(telemetryTenantRepository.findLatestConfirmedReadingSnapshot(schemaName, 10L, null))
                .thenReturn(Optional.of(new TelemetryConfirmedReadingSnapshot(new BigDecimal("200"), LocalDateTime.now().minusDays(1))));

        when(telemetryTenantRepository.findLatestPlaceholderFlowReadingIdForDate(schemaName, 10L, 1L, LocalDate.from(readingAt)))
                .thenReturn(Optional.empty());

        when(telemetryTenantRepository.createFlowReading(
                anyString(),
                anyLong(),
                anyLong(),
                any(LocalDateTime.class),
                any(BigDecimal.class),
                any(BigDecimal.class),
                anyString(),
                any(),
                any()
        )).thenReturn(99L);

        CreateReadingResponse resp = service.createReading(request, schemaName, operator, "919999999999", true);

        assertNotNull(resp);
        assertEquals(true, resp.isSuccess());
        assertEquals(new BigDecimal("100"), resp.getMeterReading());
        assertEquals("CONFIRMED", resp.getQualityStatus());

        verify(telemetryTenantRepository).createFlowReading(
                anyString(),
                anyLong(),
                anyLong(),
                any(LocalDateTime.class),
                any(BigDecimal.class),
                any(BigDecimal.class),
                anyString(),
                any(),
                any()
        );
    }
}
