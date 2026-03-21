package org.arghyam.jalsoochak.message.channel;

import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import org.arghyam.jalsoochak.message.dto.NotificationRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link SmtpMailChannel}.
 *
 * <p>Verifies that the channel delegates to {@link JavaMailSender}, returns
 * {@code true} on success, and returns {@code false} without throwing on
 * any SMTP failure (graceful degradation so callers decide retry strategy).</p>
 */
@ExtendWith(MockitoExtension.class)
class SmtpMailChannelTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private SmtpMailChannel smtpMailChannel;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(smtpMailChannel, "fromAddress", "noreply@jalsoochak.in");
        ReflectionTestUtils.setField(smtpMailChannel, "fromName", "JalSoochak");
    }

    /** Stubs {@code createMimeMessage()} — called only in tests that exercise {@code send()}. */
    private void stubMimeMessage() {
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage((jakarta.mail.Session) null));
    }

    // ─────────────────────────── channelType ───────────────────────────────────

    @Test
    void channelType_returnsSmtpEmail() {
        assertThat(smtpMailChannel.channelType()).isEqualTo("SMTP_EMAIL");
    }

    // ─────────────────────────── send — success ────────────────────────────────

    @Test
    void send_returnsTrueAndInvokesMailSender_onSuccess() {
        stubMimeMessage();
        NotificationRequest request = NotificationRequest.builder()
                .recipient("user@example.com")
                .subject("Test Subject")
                .body("<p>Hello</p>")
                .channel(SmtpMailChannel.CHANNEL_TYPE)
                .build();

        boolean result = smtpMailChannel.send(request);

        assertThat(result).isTrue();
        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void send_forwardsHtmlBodyAsIs() throws Exception {
        stubMimeMessage();
        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);

        smtpMailChannel.send(NotificationRequest.builder()
                .recipient("user@example.com")
                .subject("Invite")
                .body("<html><body>Click here</body></html>")
                .build());

        verify(mailSender).send(captor.capture());
        MimeMessage captured = captor.getValue();
        // MimeMessageHelper(message, multipart=true) wraps content in multipart/mixed →
        // multipart/related → text/html body part.
        MimeMultipart mixed = (MimeMultipart) captured.getContent();
        MimeBodyPart bodyPart = (MimeBodyPart) mixed.getBodyPart(0);
        MimeMultipart related = (MimeMultipart) bodyPart.getContent();
        MimeBodyPart htmlPart = (MimeBodyPart) related.getBodyPart(0);
        assertThat(htmlPart.getContentType()).startsWith("text/html");
        assertThat((String) htmlPart.getContent()).isEqualTo("<html><body>Click here</body></html>");
    }

    // ─────────────────────────── send — failure ────────────────────────────────

    @Test
    void send_returnsFalse_whenMailSenderThrows() {
        stubMimeMessage();
        doThrow(new MailSendException("SMTP connection refused"))
                .when(mailSender).send(any(MimeMessage.class));

        boolean result = smtpMailChannel.send(NotificationRequest.builder()
                .recipient("user@example.com")
                .subject("Test")
                .body("<p>Hi</p>")
                .build());

        assertThat(result).isFalse();
    }

    @Test
    void send_returnsFalse_whenCreateMimeMessageThrows() {
        when(mailSender.createMimeMessage()).thenThrow(new RuntimeException("Session error"));

        boolean result = smtpMailChannel.send(NotificationRequest.builder()
                .recipient("user@example.com")
                .subject("Test")
                .body("<p>Hi</p>")
                .build());

        assertThat(result).isFalse();
        // send() must not have been called since message creation itself failed
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    void send_doesNotThrow_onSmtpFailure() {
        stubMimeMessage();
        doThrow(new MailSendException("server down"))
                .when(mailSender).send(any(MimeMessage.class));

        // Must return false, never propagate the exception
        assertThat(smtpMailChannel.send(NotificationRequest.builder()
                .recipient("user@example.com")
                .subject("Subj")
                .body("Body")
                .build())).isFalse();
    }
}
