package org.arghyam.jalsoochak.user.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;

/**
 * Kafka event published to {@code common-topic} after a staff user requests an OTP.
 * {@code message-service} consumes this event and delivers the OTP via WhatsApp or SMS.
 *
 * <p>Phone number and officer name are PII — this event is published only to the
 * internal Kafka topic, never logged at INFO/WARN/ERROR level.
 */
@Value
@Builder
public class SendLoginOtpEvent {

    /** Always {@code "SEND_LOGIN_OTP"}. */
    @JsonProperty("eventType")
    String eventType;

    /** Uppercase state code, e.g. {@code "MP"}. */
    @JsonProperty("tenantCode")
    String tenantCode;

    /** Numeric tenant ID from {@code common_schema.tenant_master_table}. */
    @JsonProperty("tenantId")
    Integer tenantId;

    /** ISO-8601 timestamp of when the OTP was generated. */
    @JsonProperty("triggeredAt")
    String triggeredAt;

    /** Glific contact ID from {@code user_table.whatsapp_connection_id}. */
    @JsonProperty("glific_id")
    Long glificId;

    /** Decrypted officer name from {@code user_table.title}. PII — do not log. */
    @JsonProperty("officerName")
    String officerName;

    /** Decrypted phone number. PII — do not log at INFO/WARN/ERROR. */
    @JsonProperty("officerPhoneNumber")
    String officerPhoneNumber;

    /** Plaintext OTP to deliver. Never stored in plaintext in the DB. */
    @JsonProperty("OTP")
    String otp;

    /** {@code "WHATSAPP"} or {@code "SMS"} — from {@code STAFF_OTP_DELIVERY_CHANNEL} tenant config. */
    @JsonProperty("deliveryChannel")
    String deliveryChannel;
}
