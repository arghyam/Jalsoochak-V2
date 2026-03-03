package org.arghyam.jalsoochak.message.channel;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link GlificWhatsAppService}.
 *
 * <p>Focuses on the two-step escalation flow:
 * <ol>
 *   <li>{@code uploadMedia} – calls {@code createMessageMedia} and extracts the media ID.</li>
 *   <li>{@code sendEscalationHsm} – uploads media first, then calls
 *       {@code createAndSendMessage} with the returned media ID.</li>
 * </ol>
 * Also covers opt-in and nudge HSM delegation.
 */
@ExtendWith(MockitoExtension.class)
class GlificWhatsAppServiceTest {

    @Mock
    private GlificGraphQLClient client;

    @InjectMocks
    private GlificWhatsAppService service;

    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "nudgeTemplateId", "nudge-tmpl-1");
        ReflectionTestUtils.setField(service, "escalationTemplateId", "esc-tmpl-2");
    }

    // ──────────────────────────── optIn ────────────────────────────────────────

    @Test
    void optIn_returnsContactId_fromGlificResponse() throws Exception {
        JsonNode response = mapper.readTree(
                """
                {"optinContact":{"contact":{"id":42}}}
                """);
        when(client.execute(contains("optinContact"), anyMap())).thenReturn(response);

        Long contactId = service.optIn("919876543210");

        assertThat(contactId).isEqualTo(42L);
        verify(client).execute(contains("optinContact"), argThat(vars ->
                "919876543210".equals(vars.get("phone"))));
    }

    // ──────────────────────────── sendNudgeHsm ─────────────────────────────────

    @Test
    void sendNudgeHsm_callsSendHsmMutation_withCorrectParameters() throws Exception {
        JsonNode response = mapper.readTree("""
                {"sendHsmMessage":{"message":{"id":1,"body":"Hi","isHSM":true},"errors":[]}}
                """);
        when(client.execute(contains("sendHsmMessage"), anyMap())).thenReturn(response);

        service.sendNudgeHsm(99L, "Ramesh", "02 March 2026");

        ArgumentCaptor<Map<String, Object>> varsCaptor = varsCaptor();
        verify(client).execute(contains("sendHsmMessage"), varsCaptor.capture());

        Map<String, Object> vars = varsCaptor.getValue();
        assertThat(vars.get("templateId")).isEqualTo("nudge-tmpl-1");
        assertThat(vars.get("receiverId")).isEqualTo(99L);
        assertThat(vars.get("parameters")).isEqualTo(List.of("Ramesh", "02 March 2026"));
    }

    // ──────────────────────── uploadMedia ──────────────────────────────────────

    @Test
    void uploadMedia_callsCreateMessageMediaMutation_withUrlAndSourceUrl() throws Exception {
        JsonNode response = mapper.readTree("""
                {"createMessageMedia":{"messageMedia":{"id":"777","url":"https://example.com/r.pdf"},"errors":[]}}
                """);
        when(client.execute(contains("createMessageMedia"), anyMap())).thenReturn(response);

        String mediaId = service.uploadMedia("https://example.com/r.pdf");

        assertThat(mediaId).isEqualTo("777");

        ArgumentCaptor<Map<String, Object>> varsCaptor = varsCaptor();
        verify(client).execute(contains("createMessageMedia"), varsCaptor.capture());

        @SuppressWarnings("unchecked")
        Map<String, Object> input = (Map<String, Object>) varsCaptor.getValue().get("input");
        assertThat(input.get("url")).isEqualTo("https://example.com/r.pdf");
        assertThat(input.get("source_url")).isEqualTo("https://example.com/r.pdf");
        assertThat(input.get("isTemplateMedia")).isEqualTo(false);
    }

    @Test
    void uploadMedia_throwsException_whenClientFails() {
        when(client.execute(contains("createMessageMedia"), anyMap()))
                .thenThrow(new RuntimeException("Glific media upload failed"));

        assertThatThrownBy(() -> service.uploadMedia("https://example.com/r.pdf"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Glific media upload failed");
    }

    // ──────────────────────── sendEscalationHsm ────────────────────────────────

    @Test
    void sendEscalationHsm_uploadsMediaFirst_thenCallsCreateAndSendMessage() throws Exception {
        JsonNode uploadResponse = mapper.readTree("""
                {"createMessageMedia":{"messageMedia":{"id":"999","url":"https://minio.example.com/r.pdf"},"errors":[]}}
                """);
        JsonNode sendResponse = mapper.readTree("""
                {"createAndSendMessage":{"message":{"id":2,"body":"body","isHsm":true},"errors":[]}}
                """);

        when(client.execute(contains("createMessageMedia"), anyMap())).thenReturn(uploadResponse);
        when(client.execute(contains("createAndSendMessage"), anyMap())).thenReturn(sendResponse);

        service.sendEscalationHsm(55L, "https://minio.example.com/r.pdf", "Please review the report.");

        // Step 1: media upload
        verify(client).execute(contains("createMessageMedia"), anyMap());
        // Step 2: createAndSendMessage
        verify(client).execute(contains("createAndSendMessage"), anyMap());
    }

    @Test
    void sendEscalationHsm_passesMediaIdFromUpload_toCreateAndSendMessage() throws Exception {
        JsonNode uploadResponse = mapper.readTree("""
                {"createMessageMedia":{"messageMedia":{"id":"MEDIA-123","url":"https://minio.example.com/r.pdf"}}}
                """);
        JsonNode sendResponse = mapper.readTree("""
                {"createAndSendMessage":{"message":{"id":3,"body":"ok","isHsm":true},"errors":[]}}
                """);

        when(client.execute(contains("createMessageMedia"), anyMap())).thenReturn(uploadResponse);
        when(client.execute(contains("createAndSendMessage"), anyMap())).thenReturn(sendResponse);

        service.sendEscalationHsm(77L, "https://minio.example.com/report.pdf", "body text");

        ArgumentCaptor<Map<String, Object>> sendVarsCaptor = varsCaptor();
        verify(client).execute(contains("createAndSendMessage"), sendVarsCaptor.capture());

        Map<String, Object> sendVars = sendVarsCaptor.getValue();
        assertThat(sendVars.get("mediaId")).isEqualTo("MEDIA-123");
        assertThat(sendVars.get("templateId")).isEqualTo("esc-tmpl-2");
        assertThat(sendVars.get("receiverId")).isEqualTo(77L);
        assertThat(sendVars.get("parameters")).isEqualTo(List.of("body text"));
    }

    @Test
    void sendEscalationHsm_doesNotCallCreateAndSend_whenUploadFails() {
        when(client.execute(contains("createMessageMedia"), anyMap()))
                .thenThrow(new RuntimeException("MinIO URL unreachable"));

        assertThatThrownBy(() ->
                service.sendEscalationHsm(88L, "https://minio.example.com/r.pdf", "body"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("MinIO URL unreachable");

        verify(client, never()).execute(contains("createAndSendMessage"), anyMap());
    }

    @Test
    void sendEscalationHsm_usesBodyText_asOnlyParameter_inCreateAndSendMessage() throws Exception {
        JsonNode uploadResponse = mapper.readTree("""
                {"createMessageMedia":{"messageMedia":{"id":"M-1"}}}
                """);
        JsonNode sendResponse = mapper.readTree("""
                {"createAndSendMessage":{"message":{"id":4,"body":"ok","isHsm":true},"errors":[]}}
                """);
        when(client.execute(contains("createMessageMedia"), anyMap())).thenReturn(uploadResponse);
        when(client.execute(contains("createAndSendMessage"), anyMap())).thenReturn(sendResponse);

        service.sendEscalationHsm(11L, "https://minio.example.com/r.pdf", "Localized escalation text");

        ArgumentCaptor<Map<String, Object>> captor = varsCaptor();
        verify(client).execute(contains("createAndSendMessage"), captor.capture());

        // Body text is the sole parameter; the document attachment is via mediaId, not parameters
        @SuppressWarnings("unchecked")
        List<String> params = (List<String>) captor.getValue().get("parameters");
        assertThat(params).containsExactly("Localized escalation text");
    }

    // ────────────────────────────── helpers ────────────────────────────────────

    @SuppressWarnings("unchecked")
    private ArgumentCaptor<Map<String, Object>> varsCaptor() {
        return ArgumentCaptor.forClass(Map.class);
    }
}
