package org.arghyam.jalsoochak.message.channel;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link WhatsAppChannel}.
 *
 * <p>Verifies that the channel delegates correctly to {@link GlificWhatsAppService},
 * returns {@code true} on success, and returns {@code false} (without throwing)
 * on failure to allow the caller to handle delivery failures gracefully.</p>
 */
@ExtendWith(MockitoExtension.class)
class WhatsAppChannelTest {

    @Mock
    private GlificWhatsAppService glificWhatsAppService;

    @InjectMocks
    private WhatsAppChannel whatsAppChannel;

    // ──────────────────────────────── sendNudge ────────────────────────────────

    @Test
    void sendNudge_returnsTrueAndSendsHsm_onSuccess() {
        when(glificWhatsAppService.optIn("919876543210")).thenReturn(42L);

        boolean result = whatsAppChannel.sendNudge("919876543210", "Ramesh", "02 March 2026");

        assertThat(result).isTrue();
        verify(glificWhatsAppService).optIn("919876543210");
        verify(glificWhatsAppService).sendNudgeHsm(42L, "Ramesh", "02 March 2026");
    }

    @Test
    void sendNudge_returnsFalse_whenOptInThrows() {
        when(glificWhatsAppService.optIn(anyString()))
                .thenThrow(new RuntimeException("Glific unreachable"));

        boolean result = whatsAppChannel.sendNudge("919876543210", "Ramesh", "02 March 2026");

        assertThat(result).isFalse();
        verify(glificWhatsAppService, never()).sendNudgeHsm(anyLong(), anyString(), anyString());
    }

    @Test
    void sendNudge_returnsFalse_whenSendNudgeHsmThrows() {
        when(glificWhatsAppService.optIn(anyString())).thenReturn(99L);
        doThrow(new RuntimeException("HSM send failed"))
                .when(glificWhatsAppService).sendNudgeHsm(anyLong(), anyString(), anyString());

        boolean result = whatsAppChannel.sendNudge("919876543210", "Op", "02 March 2026");

        assertThat(result).isFalse();
    }

    @Test
    void sendNudge_passesCorrectParametersToHsm() {
        when(glificWhatsAppService.optIn("911111111111")).thenReturn(55L);

        whatsAppChannel.sendNudge("911111111111", "Suresh", "03 March 2026");

        verify(glificWhatsAppService).sendNudgeHsm(eq(55L), eq("Suresh"), eq("03 March 2026"));
    }

    // ────────────────────────────── sendDocument ───────────────────────────────

    @Test
    void sendDocument_returnsTrueAndSendsEscalationHsm_onSuccess() {
        when(glificWhatsAppService.optIn("919000000000")).thenReturn(77L);

        boolean result = whatsAppChannel.sendDocument(
                "919000000000", "https://minio.example.com/report.pdf", "Escalation body text");

        assertThat(result).isTrue();
        verify(glificWhatsAppService).optIn("919000000000");
        verify(glificWhatsAppService).sendEscalationHsm(
                77L, "https://minio.example.com/report.pdf", "Escalation body text");
    }

    @Test
    void sendDocument_returnsFalse_whenOptInThrows() {
        when(glificWhatsAppService.optIn(anyString()))
                .thenThrow(new RuntimeException("Network error"));

        boolean result = whatsAppChannel.sendDocument(
                "919000000000", "https://minio.example.com/r.pdf", "body");

        assertThat(result).isFalse();
        verify(glificWhatsAppService, never()).sendEscalationHsm(anyLong(), anyString(), anyString());
    }

    @Test
    void sendDocument_returnsFalse_whenSendEscalationHsmThrows() {
        when(glificWhatsAppService.optIn(anyString())).thenReturn(88L);
        doThrow(new RuntimeException("HSM delivery failed"))
                .when(glificWhatsAppService).sendEscalationHsm(anyLong(), anyString(), anyString());

        boolean result = whatsAppChannel.sendDocument(
                "919000000001", "https://minio.example.com/r2.pdf", "body text");

        assertThat(result).isFalse();
    }

    @Test
    void sendDocument_passesMinioUrlAndBodyText_toEscalationHsm() {
        when(glificWhatsAppService.optIn("912222222222")).thenReturn(33L);
        String minioUrl = "https://minio.example.com/escalation_L2_report.pdf";
        String body = "Please review the attached report.";

        whatsAppChannel.sendDocument("912222222222", minioUrl, body);

        verify(glificWhatsAppService).sendEscalationHsm(eq(33L), eq(minioUrl), eq(body));
    }

    // ─────────────────────────── channelType ───────────────────────────────────

    @Test
    void channelType_returnsWhatsApp() {
        assertThat(whatsAppChannel.channelType()).isEqualTo("WHATSAPP");
    }
}
