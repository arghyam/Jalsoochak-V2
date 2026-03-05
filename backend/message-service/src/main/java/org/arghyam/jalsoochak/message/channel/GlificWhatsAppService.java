package org.arghyam.jalsoochak.message.channel;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles Glific GraphQL operations: opt-in a contact and send HSM messages.
 *
 * <p>Nudge HSM: {{1}} = operator name, {{2}} = today's date.</p>
 * <p>Escalation HSM (document type, two-step):
 * <ol>
 *   <li>Upload the MinIO PDF URL via {@code createMessageMedia} → receive {@code mediaId}.</li>
 *   <li>Send via {@code createAndSendMessage} with {@code mediaId} (document header)
 *       and {@code parameters[0]} = localized body text.</li>
 * </ol>
 * </p>
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

    private static final String NUDGE_HSM_MUTATION = """
            mutation sendHsmMessage($templateId: ID!, $receiverId: ID!, $parameters: [String]) {
              sendHsmMessage(templateId: $templateId, receiverId: $receiverId, parameters: $parameters) {
                message { id body isHSM }
                errors { key message }
              }
            }""";

    private static final String CREATE_MESSAGE_MEDIA_MUTATION = """
            mutation createMessageMedia($input: MessageMediaInput!) {
              createMessageMedia(input: $input) {
                messageMedia { id url }
                errors { key message }
              }
            }""";

    private static final String START_CONTACT_FLOW_MUTATION = """
            mutation startContactFlow($flowId: ID!, $contactId: ID!, $defaultResults: Json!) {
              startContactFlow(flowId: $flowId, contactId: $contactId, defaultResults: $defaultResults) {
                success
                errors { key message }
              }
            }""";

    private static final String CREATE_AND_SEND_MESSAGE_MUTATION = """
    mutation createAndSendMessage($input: MessageInput!) {
      createAndSendMessage(input: $input) {
        message {
          id
          body
          isHsm
        }
        errors {
          key
          message
        }
      }
    }
    """;

    private final GlificGraphQLClient client;
    private final ObjectMapper objectMapper;

    @Value("${glific.template.nudge-id:}")
    private String nudgeTemplateId;

    @Value("${glific.template.escalation-id:}")
    private String escalationTemplateId;

    @Value("${glific.flow.nudge-id:}")
    private String nudgeFlowId;

    @PostConstruct
    void validateTemplates() {
        if (nudgeTemplateId == null || nudgeTemplateId.isBlank()
                || escalationTemplateId == null || escalationTemplateId.isBlank()) {
            throw new IllegalStateException("glific.template.nudge-id and glific.template.escalation-id must be configured");
        }
    }

    @Value("${glific.media.escalation-caption:Escalations}")
    private String escalationCaption;

    @Value("${glific.media.escalation-thumbnail:}")
    private String escalationThumbnail;

    /**
     * Opts in the contact by phone number and returns the Glific contact ID.
     * Phone must be in E.164 format (e.g., 919876543210).
     */
    public Long optIn(String phone) {
        log.debug("[Glific] Opting in contact");
        JsonNode response = client.execute(OPTIN_MUTATION, Map.of("phone", phone));
        checkErrors(response, "optinContact");
        return response.path("optinContact").path("contact").path("id").asLong();
    }

    /**
     * Sends the nudge HSM template to the contact.
     * Template variable {{1}} = operator name, {{2}} = today's date.
     */
    public void sendNudgeHsm(Long contactId, String operatorName, String date) {
        JsonNode response = client.execute(NUDGE_HSM_MUTATION, Map.of(
                "templateId", nudgeTemplateId,
                "receiverId", contactId,
                "parameters", List.of(operatorName, date)));
        checkErrors(response, "sendHsmMessage");
        log.debug("[Glific] Nudge HSM sent to contactId={}", contactId);
    }

    /**
     * Uploads a media file to Glific via its publicly reachable URL and returns the Glific media ID.
     *
     * @param publicUrl publicly reachable URL of the file (e.g. MinIO presigned URL or ngrok URL)
     * @return Glific {@code messageMedia.id} to pass to {@link #sendEscalationHsm}
     */
    public String uploadMedia(String publicUrl) {
        log.debug("[Glific] Uploading media");
        JsonNode response = client.execute(CREATE_MESSAGE_MEDIA_MUTATION, Map.of(
                        "input", Map.of(
                                "url", publicUrl,
                                "source_url", publicUrl,
                                "caption", escalationCaption,
                                "thumbnail", escalationThumbnail,
                                "isTemplateMedia", true)));
        checkErrors(response, "createMessageMedia");
        String mediaId = response.path("createMessageMedia").path("messageMedia").path("id").asText();
        log.info("[Glific] Media uploaded, mediaId={}", mediaId);
        return mediaId;
    }

    /**
     * Sends the escalation document HSM to the officer.
     *
     * <p>Two-step process:
     * <ol>
     *   <li>Upload {@code minioUrl} via {@code createMessageMedia} → {@code mediaId}</li>
     *   <li>Send {@code createAndSendMessage} with the {@code mediaId} as the document
     *       header attachment and {@code bodyText} as the body template parameter.</li>
     * </ol>
     *
     * @param contactId Glific contact ID of the officer
     * @param minioUrl  publicly reachable URL of the escalation PDF
     */
    public void sendEscalationHsm(Long contactId, String minioUrl) {

        String mediaId = uploadMedia(minioUrl);

        Map<String, Object> input = new HashMap<>();
        input.put("templateId", Integer.parseInt(escalationTemplateId));
        input.put("receiverId", contactId.intValue());
        input.put("isHsm", true);
        input.put("params", List.of());

        if (mediaId != null && !mediaId.isBlank()) {
            input.put("mediaId", Integer.parseInt(mediaId));
        }

        JsonNode response = client.execute(
                CREATE_AND_SEND_MESSAGE_MUTATION,
                Map.of("input", input)
        );
        checkErrors(response, "createAndSendMessage");

        log.debug("[Glific] Escalation HSM sent to contactId={}", contactId);
    }

    /**
     * Initiates a Glific flow for the nudge contact, passing operator name and date as
     * flow variables so the flow can populate the HSM template and handle button responses.
     *
     * <p>Requires {@code glific.flow.nudge-id} to be configured.
     * Falls back gracefully — if the flow ID is not set, throws {@link IllegalStateException}
     * so the caller can fall back to {@link #sendNudgeHsm}.</p>
     *
     * @param contactId    Glific contact ID (from {@link #optIn})
     * @param operatorName operator name passed as flow variable {@code operator_name}
     * @param date         today's date passed as flow variable {@code date}
     */
    public void startNudgeFlow(Long contactId, String operatorName, String date) {
        if (nudgeFlowId == null || nudgeFlowId.isBlank()) {
            throw new IllegalStateException("glific.flow.nudge-id is not configured");
        }

        String defaultResults;
        try {
            defaultResults = objectMapper.writeValueAsString(
                    Map.of("operator_name", operatorName, "date", date));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize flow defaultResults", e);
        }

        JsonNode response = client.execute(START_CONTACT_FLOW_MUTATION, Map.of(
                "flowId", nudgeFlowId,
                "contactId", contactId,
                "defaultResults", defaultResults));

        JsonNode flowNode = response.path("startContactFlow");
        if (flowNode.isMissingNode() || flowNode.isNull()) {
            throw new RuntimeException("Glific GraphQL response missing key: startContactFlow");
        }
        JsonNode errors = flowNode.path("errors");
        if (errors.isArray() && !errors.isEmpty()) {
            throw new RuntimeException("Glific startContactFlow error: " + errors);
        }
        boolean success = flowNode.path("success").asBoolean(false);
        if (!success) {
            throw new RuntimeException("Glific startContactFlow returned success=false for contactId=" + contactId);
        }
        log.debug("[Glific] Nudge flow started for contactId={}", contactId);
    }

    private void checkErrors(JsonNode response, String mutationKey) {
        JsonNode mutationNode = response.path(mutationKey);
        if (mutationNode.isMissingNode() || mutationNode.isNull()) {
            throw new RuntimeException("Glific GraphQL response missing key: " + mutationKey);
        }
        JsonNode errors = mutationNode.path("errors");
        if (errors.isArray() && !errors.isEmpty()) {
            String msg = errors.toString();
            log.error("[Glific] GraphQL errors in {}: {}", mutationKey, msg);
            throw new RuntimeException("Glific GraphQL error in " + mutationKey + ": " + msg);
        }
    }

    private static String maskPhone(String phone) {
        if (phone == null || phone.length() <= 4) return "****";
        return "****" + phone.substring(phone.length() - 4);
    }
}
