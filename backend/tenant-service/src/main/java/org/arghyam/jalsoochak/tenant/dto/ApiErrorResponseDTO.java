package org.arghyam.jalsoochak.tenant.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Standard API error response")
public class ApiErrorResponseDTO {

    @Schema(description = "Timestamp of the error")
    private String timestamp;

    @Schema(description = "HTTP status code", example = "404")
    private int status;

    @Schema(description = "HTTP error name", example = "Not Found")
    private String error;

    @Schema(description = "Error detail message")
    private String message;

    public ApiErrorResponseDTO(int status, String error, String message) {
        this.timestamp = LocalDateTime.now().toString();
        this.status = status;
        this.error = error;
        this.message = message;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public int getStatus() {
        return status;
    }

    public String getError() {
        return error;
    }

    public String getMessage() {
        return message;
    }
}
