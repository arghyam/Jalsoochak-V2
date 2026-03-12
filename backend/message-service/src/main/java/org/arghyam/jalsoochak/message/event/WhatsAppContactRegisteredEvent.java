package org.arghyam.jalsoochak.message.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WhatsAppContactRegisteredEvent {
    private String eventType;   // always "WHATSAPP_CONTACT_REGISTERED"
    private String tenantSchema;
    private Long userId;
    private Long contactId;
}
