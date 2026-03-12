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
    @Builder.Default
    private String eventType = "WHATSAPP_CONTACT_REGISTERED";   // always "WHATSAPP_CONTACT_REGISTERED"
    private String tenantSchema;
    private Long userId;
    private Long contactId;
}
