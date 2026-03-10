package org.arghyam.jalsoochak.user.dto.common;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.OffsetDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiErrorResponseDTO {

    private String timestamp;
    private int status;
    private String error;
    private String message;
    private String requestId;

    public ApiErrorResponseDTO(int status, String error, String message, String requestId) {
        this.timestamp = OffsetDateTime.now().toString();
        this.status = status;
        this.error = error;
        this.message = message;
        this.requestId = requestId;
    }

    public String getTimestamp() { return timestamp; }
    public int getStatus() { return status; }
    public String getError() { return error; }
    public String getMessage() { return message; }
    public String getRequestId() { return requestId; }
}
