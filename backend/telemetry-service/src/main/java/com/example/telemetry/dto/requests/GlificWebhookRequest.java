package com.example.telemetry.dto.requests;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class GlificWebhookRequest {

    private String contactId;

    private String messageType;

    private String mediaId;

    private String mediaUrl;

    private String confirmedReading;

    private String correlationId;
}