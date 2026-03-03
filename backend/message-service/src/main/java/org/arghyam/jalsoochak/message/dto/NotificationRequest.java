package org.arghyam.jalsoochak.message.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationRequest {

    /** Target recipient – email address, phone number, or webhook URL */
    private String recipient;

    /** Subject line (used by email channel) */
    private String subject;

    /** Message body */
    private String body;

    /** Channel to use: WEBHOOK | EMAIL | WHATSAPP */
    private String channel;
}
