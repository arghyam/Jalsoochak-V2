package com.example.telemetry.service;

import com.example.telemetry.dto.response.FlowVisionResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
public class FlowVisionService {

    private final RestTemplate restTemplate;
    private final String flowVisionUrl;
    private final int retryMaxAttempts;
    private final long retryInitialBackoffMs;

    public FlowVisionService(RestTemplate restTemplate,
                             @Value("${flowvision.url:https://jalsoochak.beehyv.com/flowvision/v1/extract-reading}") String flowVisionUrl,
                             @Value("${flowvision.retry.max-attempts:3}") int retryMaxAttempts,
                             @Value("${flowvision.retry.initial-backoff-ms:300}") long retryInitialBackoffMs) {
        this.restTemplate = restTemplate;
        this.flowVisionUrl = flowVisionUrl;
        this.retryMaxAttempts = Math.max(1, retryMaxAttempts);
        this.retryInitialBackoffMs = Math.max(0L, retryInitialBackoffMs);
    }

    public FlowVisionResult extractReading(String readingUrl) {

        for (int attempt = 1; attempt <= retryMaxAttempts; attempt++) {
            try {

                Map<String, String> payload = new HashMap<>();
                payload.put("imageURL", readingUrl);

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);

                HttpEntity<Map<String, String>> requestEntity =
                        new HttpEntity<>(payload, headers);

                ResponseEntity<Map> responseEntity = restTemplate.exchange(
                        flowVisionUrl,
                        HttpMethod.POST,
                        requestEntity,
                        Map.class
                );


                if (!responseEntity.getStatusCode().is2xxSuccessful()) {
                    log.error("FlowVision HTTP error: {}", responseEntity.getStatusCode());
                    return null;
                }

                Map<String, Object> responseBody = responseEntity.getBody();

                if (responseBody == null || !responseBody.containsKey("result")) {
                    log.error("FlowVision response missing 'result'");
                    return null;
                }

                Map<String, Object> resultMap =
                        (Map<String, Object>) responseBody.get("result");

                if (resultMap == null || !"SUCCESS".equals(resultMap.get("status"))) {
                    log.warn("FlowVision OCR not successful: {}", resultMap);
                    return null;
                }

                if (!resultMap.containsKey("data")) {
                    log.error("FlowVision result missing 'data'");
                    return null;
                }

                Map<String, Object> dataMap =
                        (Map<String, Object>) resultMap.get("data");

                BigDecimal adjustedReading = null;
                Object meterReadingObj = dataMap.get("meterReading");

                if (meterReadingObj != null) {
                    adjustedReading = new BigDecimal(meterReadingObj.toString());
                }

                String qualityStatus =
                        dataMap.getOrDefault("qualityStatus", "unknown").toString();

                BigDecimal qualityConfidence = null;
                Object confidenceObj = dataMap.get("qualityConfidence");

                if (confidenceObj != null) {
                    qualityConfidence = new BigDecimal(confidenceObj.toString());
                }

                String correlationId =
                        resultMap.getOrDefault(
                                "correlationId",
                                UUID.randomUUID().toString()
                        ).toString();
                log.debug("FlowVision OCR succeeded with correlationId={}", correlationId);

                return FlowVisionResult.builder()
                        .adjustedReading(adjustedReading)
                        .qualityStatus(qualityStatus)
                        .qualityConfidence(qualityConfidence)
                        .correlationId(correlationId)
                        .build();

            } catch (RestClientException ex) {
                if (attempt == retryMaxAttempts) {
                    log.error("FlowVision OCR call failed for image {} after {} attempts", readingUrl, attempt, ex);
                    return null;
                }
                long backoffMs = retryInitialBackoffMs * (1L << (attempt - 1));
                log.warn("FlowVision OCR attempt {} failed for image {}. Retrying in {} ms", attempt, readingUrl, backoffMs);
                sleepBackoff(backoffMs);
            } catch (Exception ex) {
                log.error("FlowVision OCR call failed for image {}", readingUrl, ex);
                return null;
            }
        }
        return null;
    }

    private void sleepBackoff(long backoffMs) {
        if (backoffMs <= 0) {
            return;
        }
        try {
            Thread.sleep(backoffMs);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

}
