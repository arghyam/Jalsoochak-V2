package com.example.message.channel;

import com.example.message.dto.NotificationRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * WhatsApp channel powered by <strong>Glific</strong> GraphQL HSM API.
 *
 * <p>Nudges use a text HSM template with {@code {{1}}} = localized message body.</p>
 * <p>Escalations use a document HSM template with {@code {{1}}} = MinIO URL
 * and {@code {{2}}} = localized body text.</p>
 *
 * <p>Configure Glific credentials and template IDs via environment variables:
 * {@code GLIFIC_API_URL}, {@code GLIFIC_API_KEY},
 * {@code GLIFIC_NUDGE_TEMPLATE_ID}, {@code GLIFIC_ESCALATION_TEMPLATE_ID}.</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WhatsAppChannel implements NotificationChannel {

    private final GlificWhatsAppService glificWhatsAppService;

    @Override
    public String channelType() {
        return "WHATSAPP";
    }

    @Override
    public boolean send(NotificationRequest request) {
        try {
            Long contactId = glificWhatsAppService.optIn(request.getRecipient());
            glificWhatsAppService.sendNudgeHsm(contactId, request.getBody());
            log.info("[WHATSAPP] Nudge HSM sent");
            log.debug("[WHATSAPP] Nudge HSM sent to {}", request.getRecipient());
            return true;
        } catch (Exception ex) {
            log.error("[WHATSAPP] Failed nudge delivery: {}", ex.getMessage(), ex);
            return false;
        }
    }

    /**
     * Sends the escalation PDF (document HSM) to the officer via Glific.
     *
     * @param toPhone     recipient WhatsApp phone number (E.164 format)
     * @param documentUrl publicly reachable MinIO URL of the escalation PDF
     * @param bodyText    localized body text for the HSM template {@code {{2}}}
     * @return {@code true} if the message was accepted by Glific
     */
    public boolean sendDocument(String toPhone, String documentUrl, String bodyText) {
        try {
            Long contactId = glificWhatsAppService.optIn(toPhone);
            glificWhatsAppService.sendEscalationHsm(contactId, documentUrl, bodyText);
            log.info("[WHATSAPP] Escalation HSM sent");
            log.debug("[WHATSAPP] Escalation HSM sent to {}", toPhone);
            return true;
        } catch (Exception ex) {
            log.error("[WHATSAPP] Failed escalation delivery: {}", ex.getMessage(), ex);
            return false;
        }
    }
}
