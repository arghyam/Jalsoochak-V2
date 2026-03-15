package org.arghyam.jalsoochak.tenant.dto.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

@Schema(description = "Standard API error response")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiErrorResponseDTO {

    @Schema(description = "Timestamp of the error")
    private String timestamp;

    @Schema(description = "HTTP status code", example = "400")
    private int status;

    @Schema(description = "HTTP error name", example = "Bad Request")
    private String error;

    @Schema(description = "Error detail message")
    private String message;

    @Schema(description = "Request correlation ID")
    private String requestId;

    @Schema(description = "Field-level validation errors")
    private Object fieldErrors;

    public ApiErrorResponseDTO(int status, String error, String message, String requestId, Object fieldErrors) {
        this.timestamp = OffsetDateTime.now().toString();
        this.status = status;
        this.error = error;
        this.message = message;
        this.requestId = requestId;
        this.fieldErrors = fieldErrors;
    }

    public String getTimestamp() { return timestamp; }
    public int getStatus() { return status; }
    public String getError() { return error; }
    public String getMessage() { return message; }
    public String getRequestId() { return requestId; }
    public Object getFieldErrors() { return fieldErrors; }
}
