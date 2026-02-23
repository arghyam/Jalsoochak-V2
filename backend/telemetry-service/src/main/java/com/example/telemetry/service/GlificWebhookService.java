package com.example.telemetry.service;

import com.example.telemetry.dto.response.ClosingResponse;
import com.example.telemetry.dto.response.CreateReadingResponse;
import com.example.telemetry.dto.response.IntroResponse;
import com.example.telemetry.dto.requests.ClosingRequest;
import com.example.telemetry.dto.requests.CreateReadingRequest;
import com.example.telemetry.dto.requests.GlificWebhookRequest;
import com.example.telemetry.dto.requests.IntroRequest;
import com.example.telemetry.repository.TelemetryOperatorWithSchema;
import com.example.telemetry.repository.TelemetryTenantRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;

@Slf4j
@Service
public class GlificWebhookService {

    private final MinioService minioService;
    private final RestTemplate restTemplate;
    private final BfmReadingService bfmReadingService;
    private final TelemetryTenantRepository telemetryTenantRepository;

    private final String glificApiToken;

    public GlificWebhookService(MinioService minioService,
                                RestTemplate restTemplate,
                                BfmReadingService bfmReadingService,
                                TelemetryTenantRepository telemetryTenantRepository,
                                @Value("${glific.api-token:}") String glificApiToken) {
        this.minioService = minioService;
        this.restTemplate = restTemplate;
        this.bfmReadingService = bfmReadingService;
        this.telemetryTenantRepository = telemetryTenantRepository;
        this.glificApiToken = glificApiToken;
    }

    public CreateReadingResponse processImage(GlificWebhookRequest glificWebhookRequest) {
        try {
            String contactId = glificWebhookRequest.getContactId();
            String mediaId = glificWebhookRequest.getMediaId();
            String mediaUrl = glificWebhookRequest.getMediaUrl();

            boolean hasImage = (mediaId != null && !mediaId.isBlank()) || (mediaUrl != null && !mediaUrl.isBlank());
            if (!hasImage) {
                return CreateReadingResponse.builder()
                        .success(false)
                        .message("Invalid media. Please send a clear meter image.")
                        .qualityStatus("REJECTED")
                        .correlationId(contactId)
                        .build();
            }

            byte[] imageBytes = mediaId != null && !mediaId.isBlank()
                    ? downloadImageFromGlific(mediaId)
                    : downloadImage(mediaUrl);

            String objectKey = "bfm/" + contactId + "/" + System.currentTimeMillis() + ".jpg";
            String imageStorageUrl = minioService.upload(imageBytes, objectKey);

            TelemetryOperatorWithSchema operatorWithSchema = telemetryTenantRepository
                    .findOperatorByPhoneAcrossTenants(contactId)
                    .orElseThrow(() -> new IllegalStateException("No operator found for contactId " + contactId));

            Long schemeId = telemetryTenantRepository
                    .findFirstSchemeForUser(operatorWithSchema.schemaName(), operatorWithSchema.operator().id())
                    .orElseThrow(() -> new IllegalStateException("Operator is not mapped to any scheme"));

            CreateReadingRequest createReadingRequest = CreateReadingRequest.builder()
                    .schemeId(schemeId)
                    .operatorId(operatorWithSchema.operator().id())
                    .readingUrl(imageStorageUrl)
                    .readingValue(null)
                    .readingTime(null)
                    .build();

            return bfmReadingService.createReading(
                    createReadingRequest,
                    operatorWithSchema.schemaName(),
                    operatorWithSchema.operator(),
                    contactId
            );
        } catch (Exception e) {
            log.error("Unexpected error processing image for contactId {}: {}", glificWebhookRequest.getContactId(), e.getMessage(), e);
            return CreateReadingResponse.builder()
                    .success(false)
                    .message("Something went wrong. Please try again.")
                    .qualityStatus("REJECTED")
                    .correlationId(glificWebhookRequest.getContactId())
                    .build();
        }
    }

    public IntroResponse introMessage(IntroRequest introRequest) {
        try {
            TelemetryOperatorWithSchema operatorWithSchema = telemetryTenantRepository
                    .findOperatorByPhoneAcrossTenants(introRequest.getContactId())
                    .orElseThrow(() -> new IllegalStateException("Operator not found"));
            String name = operatorWithSchema.operator().title() != null && !operatorWithSchema.operator().title().isBlank()
                    ? operatorWithSchema.operator().title()
                    : "there";
            return IntroResponse.builder()
                    .success(true)
                    .message("Hello " + name + ", please send a clear meter reading image.")
                    .build();
        } catch (Exception e) {
            log.error("Error sending intro message for contactId {}: {}", introRequest.getContactId(), e.getMessage(), e);
            return IntroResponse.builder()
                    .success(false)
                    .message("Something went wrong. Please try again.")
                    .build();
        }
    }

    public ClosingResponse closingMessage(ClosingRequest closingRequest) {
        try {
            return ClosingResponse.builder()
                    .success(true)
                    .message("Thank you. Your reading has been recorded.")
                    .build();
        } catch (Exception e) {
            log.error("Error sending closing message for contactId {}: {}", closingRequest.getContactId(), e.getMessage(), e);
            return ClosingResponse.builder()
                    .success(false)
                    .message("Something went wrong. Please try again.")
                    .build();
        }
    }

    private byte[] downloadImageFromGlific(String mediaId) throws IOException {
        try {
            HttpHeaders headers = new HttpHeaders();
            if (glificApiToken != null && !glificApiToken.isBlank()) {
                headers.setBearerAuth(glificApiToken);
            }
            headers.set(HttpHeaders.USER_AGENT, "WaterSupplyBot/1.0");
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<byte[]> response = restTemplate.exchange(
                    "https://api.glific.org/v1/media/" + mediaId,
                    HttpMethod.GET,
                    entity,
                    byte[].class
            );

            if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
                throw new IOException("Failed to download image from Glific, status: " + response.getStatusCode());
            }
            return response.getBody();
        } catch (RestClientException e) {
            throw new IOException("Failed to download image from Glific: " + e.getMessage(), e);
        }
    }

    private byte[] downloadImage(String url) throws IOException {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set(HttpHeaders.USER_AGENT, "WaterSupplyBot/1.0");
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<byte[]> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    byte[].class
            );

            if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
                throw new IOException("Failed to download image, status: " + response.getStatusCode());
            }
            return response.getBody();
        } catch (RestClientException e) {
            throw new IOException("Failed to download image: " + e.getMessage(), e);
        }
    }
}
