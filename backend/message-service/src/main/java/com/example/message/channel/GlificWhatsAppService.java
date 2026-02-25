package com.example.message.channel;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Handles Glific GraphQL operations: opt-in a contact and send HSM messages.
 *
 * <p>Nudge HSM: single variable {{1}} = localized message text.</p>
 * <p>Escalation HSM: document type, {{1}} = MinIO URL, {{2}} = localized body text.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GlificWhatsAppService {

    private static final String OPTIN_MUTATION = """
            mutation optinContact($phone: String!) {
              optinContact(phone: $phone) {
                contact { id }
                errors { key message }
              }
            }""";

    private static final String HSM_MUTATION = """
            mutation sendHsmMessage($templateId: ID!, $receiverId: ID!, $parameters: [String]) {
              sendHsmMessage(templateId: $templateId, receiverId: $receiverId, parameters: $parameters) {
                message { id body isHSM }
                errors { key message }
              }
            }""";

    private final GlificGraphQLClient client;

    @Value("${glific.template.nudge-id:}")
    private String nudgeTemplateId;

    @Value("${glific.template.escalation-id:}")
    private String escalationTemplateId;

    /**
     * Opts in the contact by phone number and returns the Glific contact ID.
     * Phone must be in E.164 format (e.g., 919876543210).
     */
    public Long optIn(String phone) {
        log.debug("[Glific] Opting in contact");
        return client.execute(OPTIN_MUTATION, Map.of("phone", phone))
                .path("optinContact").path("contact").path("id").asLong();
    }

    /**
     * Sends the nudge HSM template to the contact.
     * Template variable {{1}} = localized message text.
     */
    public void sendNudgeHsm(Long contactId, String messageBody) {
        client.execute(HSM_MUTATION, Map.of(
                "templateId", nudgeTemplateId,
                "receiverId", contactId,
                "parameters", List.of(messageBody)));
        log.debug("[Glific] Nudge HSM sent to contactId={}", contactId);
    }

    /**
     * Sends the escalation document HSM template to the contact.
     * Template variable {{1}} = MinIO document URL, {{2}} = localized body text.
     */
    public void sendEscalationHsm(Long contactId, String minioUrl, String bodyText) {
        client.execute(HSM_MUTATION, Map.of(
                "templateId", escalationTemplateId,
                "receiverId", contactId,
                "parameters", List.of(minioUrl, bodyText)));
        log.debug("[Glific] Escalation HSM sent to contactId={}", contactId);
    }

    private static String maskPhone(String phone) {
        if (phone == null || phone.length() <= 4) return "****";
        return "****" + phone.substring(phone.length() - 4);
    }
}
