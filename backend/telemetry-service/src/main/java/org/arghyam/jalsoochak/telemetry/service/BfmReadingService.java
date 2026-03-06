package org.arghyam.jalsoochak.telemetry.service;

import org.arghyam.jalsoochak.telemetry.config.TenantContext;
import org.arghyam.jalsoochak.telemetry.dto.response.CreateReadingResponse;
import org.arghyam.jalsoochak.telemetry.dto.response.FlowVisionResult;
import org.arghyam.jalsoochak.telemetry.dto.requests.CreateReadingRequest;
import org.arghyam.jalsoochak.telemetry.repository.TelemetryConfirmedReadingSnapshot;
import org.arghyam.jalsoochak.telemetry.repository.TelemetryOperator;
import org.arghyam.jalsoochak.telemetry.repository.TelemetryReadingRecord;
import org.arghyam.jalsoochak.telemetry.repository.TelemetryTenantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class BfmReadingService {

    private final TelemetryTenantRepository telemetryTenantRepository;
    private final FlowVisionService flowVisionService;

    public CreateReadingResponse createReading(CreateReadingRequest request,
                                               String schemaName,
                                               TelemetryOperator operator,
                                               String contactId) {
        if (!telemetryTenantRepository.existsSchemeById(schemaName, request.getSchemeId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "State scheme not found");
        }

        TelemetryOperator operatorInRequest = telemetryTenantRepository
                .findOperatorById(schemaName, request.getOperatorId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Operator not found"));

        boolean belongsToScheme = telemetryTenantRepository
                .isOperatorMappedToScheme(schemaName, operatorInRequest.id(), request.getSchemeId());

        if (!belongsToScheme) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Operator does not belong to the specified scheme");
        }

        FlowVisionResult ocrResult = null;
        BigDecimal finalReading = request.getReadingValue();
        BigDecimal confidenceLevel = null;
        String message = "Reading created successfully";

        if (finalReading == null) {
            if (request.getReadingUrl() == null || request.getReadingUrl().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Either readingValue or readingUrl must be provided");
            }

            try {
                ocrResult = flowVisionService.extractReading(request.getReadingUrl());
                if (ocrResult == null || ocrResult.getAdjustedReading() == null) {
                    int retries = telemetryTenantRepository.countAnomaliesByTypeForToday(
                            schemaName,
                            operatorInRequest.id(),
                            request.getSchemeId(),
                            AnomalyConstants.TYPE_UNREADABLE_IMAGE
                    ) + 1;
                    telemetryTenantRepository.createAnomalyRecord(
                            schemaName,
                            AnomalyConstants.TYPE_UNREADABLE_IMAGE,
                            operatorInRequest.id(),
                            request.getSchemeId(),
                            null,
                            null,
                            null,
                            retries,
                            null,
                            null,
                            0,
                            "Unreadable image. OCR could not extract a valid meter reading.",
                            AnomalyConstants.STATUS_OPEN
                    );
                    return CreateReadingResponse.builder()
                            .success(false)
                            .message("Could not read meter value from image. Please retry with a clearer photo.")
                            .correlationId(UUID.randomUUID().toString())
                            .qualityStatus("REJECTED")
                            .build();
                }
                finalReading = ocrResult.getAdjustedReading();
                confidenceLevel = ocrResult.getQualityConfidence();
            } catch (Exception ex) {
                log.error("FlowVision OCR failed for URL: {}", request.getReadingUrl(), ex);
                int retries = telemetryTenantRepository.countAnomaliesByTypeForToday(
                        schemaName,
                        operatorInRequest.id(),
                        request.getSchemeId(),
                        AnomalyConstants.TYPE_UNREADABLE_IMAGE
                ) + 1;
                telemetryTenantRepository.createAnomalyRecord(
                        schemaName,
                        AnomalyConstants.TYPE_UNREADABLE_IMAGE,
                        operatorInRequest.id(),
                        request.getSchemeId(),
                        null,
                        null,
                        null,
                        retries,
                        null,
                        null,
                        0,
                        "Unreadable image. OCR failed during extraction.",
                        AnomalyConstants.STATUS_OPEN
                );
                return CreateReadingResponse.builder()
                        .success(false)
                        .message("OCR failed. Please try again with a clearer image.")
                        .correlationId(UUID.randomUUID().toString())
                        .qualityStatus("REJECTED")
                        .build();
            }
        }

        boolean hasPositiveReading = finalReading != null
                && finalReading.compareTo(BigDecimal.ZERO) > 0;
        boolean hasAcceptableConfidence = confidenceLevel == null
                || confidenceLevel.compareTo(BigDecimal.valueOf(0.7)) >= 0;
        boolean isValid = hasPositiveReading && hasAcceptableConfidence;

        String correlationId = Optional.ofNullable(ocrResult)
                .map(FlowVisionResult::getCorrelationId)
                .orElse(UUID.randomUUID().toString());
        LocalDateTime readingAt = Optional.ofNullable(request.getReadingTime()).orElse(LocalDateTime.now());

        BigDecimal extractedReading = Optional.ofNullable(ocrResult)
                .map(FlowVisionResult::getAdjustedReading)
                .orElse(finalReading);
        BigDecimal confirmedReading = request.getReadingValue() != null ? request.getReadingValue() : finalReading;

        Optional<TelemetryConfirmedReadingSnapshot> previousSnapshotOpt = telemetryTenantRepository
                .findLatestConfirmedReadingSnapshot(schemaName, request.getSchemeId(), null);

        if (previousSnapshotOpt.isPresent() && confirmedReading != null
                && confirmedReading.compareTo(previousSnapshotOpt.get().confirmedReading()) < 0) {
            TelemetryConfirmedReadingSnapshot previousSnapshot = previousSnapshotOpt.get();
            telemetryTenantRepository.createAnomalyRecord(
                    schemaName,
                    AnomalyConstants.TYPE_READING_LESS_THAN_PREVIOUS,
                    operatorInRequest.id(),
                    request.getSchemeId(),
                    extractedReading,
                    confidenceLevel,
                    confirmedReading,
                    0,
                    previousSnapshot.confirmedReading(),
                    previousSnapshot.createdAt(),
                    0,
                    "Submitted reading is less than previous confirmed reading.",
                    AnomalyConstants.STATUS_OPEN
            );
            return CreateReadingResponse.builder()
                    .success(false)
                    .message("Reading cannot be less than previous reading.")
                    .correlationId(UUID.randomUUID().toString())
                    .meterReading(confirmedReading)
                    .qualityStatus("REJECTED")
                    .lastConfirmedReading(previousSnapshot.confirmedReading())
                    .build();
        }

        if (previousSnapshotOpt.isPresent() && extractedReading != null
                && extractedReading.compareTo(previousSnapshotOpt.get().confirmedReading()) == 0
                && request.getReadingUrl() != null && !request.getReadingUrl().isBlank()) {
            TelemetryConfirmedReadingSnapshot previousSnapshot = previousSnapshotOpt.get();
            telemetryTenantRepository.createAnomalyRecord(
                    schemaName,
                    AnomalyConstants.TYPE_DUPLICATE_IMAGE_SUBMISSION,
                    operatorInRequest.id(),
                    request.getSchemeId(),
                    extractedReading,
                    confidenceLevel,
                    confirmedReading,
                    0,
                    previousSnapshot.confirmedReading(),
                    previousSnapshot.createdAt(),
                    0,
                    "Duplicate image submission detected. Extracted reading matches previous confirmed reading.",
                    AnomalyConstants.STATUS_OPEN
            );
            return CreateReadingResponse.builder()
                    .success(false)
                    .message("Duplicate image submission detected. The extracted reading matches the previous reading.")
                    .correlationId(correlationId)
                    .meterReading(confirmedReading)
                    .qualityConfidence(confidenceLevel)
                    .qualityStatus("REJECTED")
                    .lastConfirmedReading(previousSnapshot.confirmedReading())
                    .build();
        }

        Long readingId = telemetryTenantRepository.createFlowReading(
                schemaName,
                request.getSchemeId(),
                operatorInRequest.id(),
                readingAt,
                extractedReading,
                confirmedReading,
                correlationId,
                request.getReadingUrl(),
                request.getMeterChangeReason()
        );

        BigDecimal lastConfirmedReading = previousSnapshotOpt
                .map(TelemetryConfirmedReadingSnapshot::confirmedReading)
                .orElseGet(() -> telemetryTenantRepository
                        .findLastConfirmedReading(schemaName, request.getSchemeId(), readingId)
                        .orElse(null));

        String finalMessage;
        String readingText = finalReading != null ? finalReading.stripTrailingZeros().toPlainString() : null;
        if (isValid) {
            if (ocrResult != null && readingText != null) {
                finalMessage = "Reading captured successfully. Extracted reading: " + readingText;
            } else {
                finalMessage = "Reading captured successfully";
            }
        } else if (!hasPositiveReading) {
            finalMessage = "Invalid reading value";
        } else if (ocrResult != null && readingText != null) {
            finalMessage = "Low OCR confidence. Extracted reading: " + readingText + ". Please confirm reading.";
        } else {
            finalMessage = "Low OCR confidence. Please confirm reading.";
        }

        return CreateReadingResponse.builder()
                .success(hasPositiveReading)
                .message(messageOverride(contactId, finalMessage))
                .correlationId(correlationId)
                .meterReading(finalReading)
                .qualityConfidence(confidenceLevel)
                .qualityStatus(ocrResult != null ? ocrResult.getQualityStatus() : (isValid ? "CONFIRMED" : "REVIEW"))
                .lastConfirmedReading(lastConfirmedReading)
                .build();
    }

    @Transactional
    public CreateReadingResponse updateConfirmedReading(String correlationId, BigDecimal confirmedReading) {
        String schemaName = TenantContext.getSchema();
        if (schemaName == null || schemaName.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tenant could not be resolved");
        }

        if (correlationId == null || correlationId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "correlationId must be provided");
        }

        if (confirmedReading == null || confirmedReading.compareTo(BigDecimal.ZERO) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "confirmedReading must be a non-negative number");
        }

        TelemetryReadingRecord reading = telemetryTenantRepository
                .findReadingByCorrelationId(schemaName, correlationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reading not found"));

        telemetryTenantRepository.updateConfirmedReading(
                schemaName,
                reading.id(),
                confirmedReading,
                reading.createdBy() != null ? reading.createdBy() : 1L
        );

        return CreateReadingResponse.builder()
                .success(true)
                .message("Reading updated successfully")
                .correlationId(reading.correlationId())
                .meterReading(confirmedReading)
                .qualityStatus("CONFIRMED")
                .build();
    }

    private String messageOverride(String contactId, String fallbackMessage) {
        if (contactId == null || contactId.isBlank()) {
            return fallbackMessage;
        }
        return fallbackMessage;
    }
}
