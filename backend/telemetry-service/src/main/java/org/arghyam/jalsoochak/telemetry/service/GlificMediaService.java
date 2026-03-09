package org.arghyam.jalsoochak.telemetry.service;

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
public class GlificMediaService {

    private final MinioService minioService;
    private final RestTemplate restTemplate;
    private final String glificApiToken;
    private final String glificMediaBaseUrl;
    private final int mediaDownloadRetryMaxAttempts;
    private final long mediaDownloadRetryInitialBackoffMs;

    public GlificMediaService(MinioService minioService,
                              RestTemplate restTemplate,
                              @Value("${glific.media-base-url:https://api.glific.org/v1/media}") String glificMediaBaseUrl,
                              @Value("${media-download.retry.max-attempts:3}") int mediaDownloadRetryMaxAttempts,
                              @Value("${media-download.retry.initial-backoff-ms:300}") long mediaDownloadRetryInitialBackoffMs,
                              @Value("${glific.api-token:}") String glificApiToken) {
        this.minioService = minioService;
        this.restTemplate = restTemplate;
        this.glificMediaBaseUrl = glificMediaBaseUrl.endsWith("/")
                ? glificMediaBaseUrl.substring(0, glificMediaBaseUrl.length() - 1)
                : glificMediaBaseUrl;
        this.mediaDownloadRetryMaxAttempts = Math.max(1, mediaDownloadRetryMaxAttempts);
        this.mediaDownloadRetryInitialBackoffMs = Math.max(0L, mediaDownloadRetryInitialBackoffMs);
        this.glificApiToken = glificApiToken;
    }

    public byte[] downloadImage(String mediaId, String mediaUrl) throws IOException {
        boolean hasImage = (mediaId != null && !mediaId.isBlank()) || (mediaUrl != null && !mediaUrl.isBlank());
        if (!hasImage) {
            throw new IllegalStateException("Invalid media. Please send a clear meter image.");
        }
        return mediaId != null && !mediaId.isBlank()
                ? downloadImageFromGlific(mediaId)
                : downloadImageFromUrl(mediaUrl);
    }

    public String uploadImage(String contactId, byte[] imageBytes) {
        String objectKey = "bfm/" + contactId + "/" + System.currentTimeMillis() + ".jpg";
        String imageStorageUrl = minioService.upload(imageBytes, objectKey);
        log.info("imageStorageUrl: {}", imageStorageUrl);
        log.debug("Image uploaded for contactId {} with objectKey {}", contactId, objectKey);
        return imageStorageUrl;
    }

    private byte[] downloadImageFromGlific(String mediaId) throws IOException {
        for (int attempt = 1; attempt <= mediaDownloadRetryMaxAttempts; attempt++) {
            try {
                HttpHeaders headers = new HttpHeaders();
                if (glificApiToken != null && !glificApiToken.isBlank()) {
                    headers.setBearerAuth(glificApiToken);
                }
                headers.set(HttpHeaders.USER_AGENT, "WaterSupplyBot/1.0");
                HttpEntity<Void> entity = new HttpEntity<>(headers);

                ResponseEntity<byte[]> response = restTemplate.exchange(
                        glificMediaBaseUrl + "/" + mediaId,
                        HttpMethod.GET,
                        entity,
                        byte[].class
                );

                if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
                    throw new IOException("Failed to download image from Glific, status: " + response.getStatusCode());
                }
                return response.getBody();
            } catch (RestClientException e) {
                if (attempt == mediaDownloadRetryMaxAttempts) {
                    throw new IOException("Failed to download image from Glific after " + attempt + " attempts: " + e.getMessage(), e);
                }
                long backoffMs = mediaDownloadRetryInitialBackoffMs * (1L << (attempt - 1));
                log.warn("Glific media download attempt {} failed for mediaId {}. Retrying in {} ms", attempt, mediaId, backoffMs);
                sleepBackoff(backoffMs);
            }
        }
        throw new IOException("Failed to download image from Glific");
    }

    private byte[] downloadImageFromUrl(String url) throws IOException {
        for (int attempt = 1; attempt <= mediaDownloadRetryMaxAttempts; attempt++) {
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
                if (attempt == mediaDownloadRetryMaxAttempts) {
                    throw new IOException("Failed to download image after " + attempt + " attempts: " + e.getMessage(), e);
                }
                long backoffMs = mediaDownloadRetryInitialBackoffMs * (1L << (attempt - 1));
                log.warn("Media download attempt {} failed for URL {}. Retrying in {} ms", attempt, url, backoffMs);
                sleepBackoff(backoffMs);
            }
        }
        throw new IOException("Failed to download image");
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
