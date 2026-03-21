package org.arghyam.jalsoochak.message.service;

import org.arghyam.jalsoochak.message.channel.SmtpMailChannel;
import org.arghyam.jalsoochak.message.dto.NotificationRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link AccountEmailService}.
 *
 * <p>Verifies that each public send-method delegates to {@link SmtpMailChannel}
 * with the correct recipient, subject, and HTML body, and that a failed SMTP
 * send (channel returns {@code false}) surfaces as a {@link RuntimeException}
 * so callers can trigger Kafka retry.</p>
 */
@ExtendWith(MockitoExtension.class)
class AccountEmailServiceTest {

    @Mock
    private SmtpMailChannel smtpMailChannel;

    @InjectMocks
    private AccountEmailService accountEmailService;

    @BeforeEach
    void setUp() {
        when(smtpMailChannel.send(any())).thenReturn(true);
    }

    // ─────────────────────────── sendInviteEmail ───────────────────────────────

    @Test
    void sendInviteEmail_usesStateAdminSubject_forStateAdminRole() {
        ArgumentCaptor<NotificationRequest> captor = ArgumentCaptor.forClass(NotificationRequest.class);

        accountEmailService.sendInviteEmail(
                "admin@state.gov", "Ravi Kumar", "STATE_ADMIN",
                "https://app.jalsoochak.in/activate?token=abc", 24);

        verify(smtpMailChannel).send(captor.capture());
        NotificationRequest req = captor.getValue();
        assertThat(req.getRecipient()).isEqualTo("admin@state.gov");
        assertThat(req.getSubject()).contains("State System Admin");
        assertThat(req.getBody()).contains("Activate Account");
        assertThat(req.getBody()).contains("https://app.jalsoochak.in/activate?token=abc");
        assertThat(req.getBody()).contains("24");
        assertThat(req.getBody()).contains("Ravi Kumar");
    }

    @Test
    void sendInviteEmail_usesSuperUserSubject_forSuperUserRole() {
        ArgumentCaptor<NotificationRequest> captor = ArgumentCaptor.forClass(NotificationRequest.class);

        accountEmailService.sendInviteEmail(
                "su@arghyam.in", "Priya", "SUPER_USER",
                "https://app.jalsoochak.in/activate?token=xyz", 48);

        verify(smtpMailChannel).send(captor.capture());
        assertThat(captor.getValue().getSubject()).contains("Super User");
    }

    @Test
    void sendInviteEmail_usesDefaultSubject_forUnknownRole() {
        ArgumentCaptor<NotificationRequest> captor = ArgumentCaptor.forClass(NotificationRequest.class);

        accountEmailService.sendInviteEmail(
                "op@tenant.in", "Mohan", "FIELD_OFFICER",
                "https://app.jalsoochak.in/activate?token=def", 12);

        verify(smtpMailChannel).send(captor.capture());
        assertThat(captor.getValue().getSubject()).isEqualTo("You are invited to join JalSoochak");
    }

    @Test
    void sendInviteEmail_usesDefaultSubject_whenRoleIsNull() {
        ArgumentCaptor<NotificationRequest> captor = ArgumentCaptor.forClass(NotificationRequest.class);

        accountEmailService.sendInviteEmail(
                "op@tenant.in", "Mohan", null,
                "https://app.jalsoochak.in/activate?token=def", 12);

        verify(smtpMailChannel).send(captor.capture());
        assertThat(captor.getValue().getSubject()).isEqualTo("You are invited to join JalSoochak");
    }

    @Test
    void sendInviteEmail_substitutesUserGreeting_whenNameIsNull() {
        ArgumentCaptor<NotificationRequest> captor = ArgumentCaptor.forClass(NotificationRequest.class);

        accountEmailService.sendInviteEmail(
                "op@tenant.in", null, "STATE_ADMIN",
                "https://app.jalsoochak.in/activate?token=def", 24);

        verify(smtpMailChannel).send(captor.capture());
        assertThat(captor.getValue().getBody()).contains("Dear User,");
    }

    @Test
    void sendInviteEmail_substitutesUserGreeting_whenNameIsBlank() {
        ArgumentCaptor<NotificationRequest> captor = ArgumentCaptor.forClass(NotificationRequest.class);

        accountEmailService.sendInviteEmail(
                "op@tenant.in", "   ", "STATE_ADMIN",
                "https://app.jalsoochak.in/activate?token=def", 24);

        verify(smtpMailChannel).send(captor.capture());
        assertThat(captor.getValue().getBody()).contains("Dear User,");
    }

    @Test
    void sendInviteEmail_setsChannelToSmtpEmail() {
        ArgumentCaptor<NotificationRequest> captor = ArgumentCaptor.forClass(NotificationRequest.class);

        accountEmailService.sendInviteEmail(
                "op@tenant.in", "Dev", "STATE_ADMIN",
                "https://link", 24);

        verify(smtpMailChannel).send(captor.capture());
        assertThat(captor.getValue().getChannel()).isEqualTo(SmtpMailChannel.CHANNEL_TYPE);
    }

    @Test
    void sendInviteEmail_throwsRuntimeException_whenSmtpChannelReturnsFalse() {
        when(smtpMailChannel.send(any())).thenReturn(false);

        assertThatThrownBy(() -> accountEmailService.sendInviteEmail(
                "op@tenant.in", "Dev", "STATE_ADMIN", "https://link", 24))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to send email");
    }

    @Test
    void sendInviteEmail_escapesHtmlInName() {
        ArgumentCaptor<NotificationRequest> captor = ArgumentCaptor.forClass(NotificationRequest.class);

        accountEmailService.sendInviteEmail(
                "admin@state.gov", "<script>alert('xss')</script>", "STATE_ADMIN",
                "https://app.jalsoochak.in/activate?token=abc", 24);

        verify(smtpMailChannel).send(captor.capture());
        assertThat(captor.getValue().getBody()).doesNotContain("<script>");
        assertThat(captor.getValue().getBody()).contains("&lt;script&gt;");
    }

    @Test
    void sendInviteEmail_escapesHtmlInInviteLink() {
        ArgumentCaptor<NotificationRequest> captor = ArgumentCaptor.forClass(NotificationRequest.class);

        accountEmailService.sendInviteEmail(
                "admin@state.gov", "Ravi", "STATE_ADMIN",
                "https://app.jalsoochak.in/activate?token=abc&next=<evil>", 24);

        verify(smtpMailChannel).send(captor.capture());
        assertThat(captor.getValue().getBody()).doesNotContain("<evil>");
        assertThat(captor.getValue().getBody()).contains("&amp;next=&lt;evil&gt;");
    }

    // ─────────────────────────── sendReinviteEmail ─────────────────────────────

    @Test
    void sendReinviteEmail_setsReminderSubjectAndContainsLink() {
        ArgumentCaptor<NotificationRequest> captor = ArgumentCaptor.forClass(NotificationRequest.class);

        accountEmailService.sendReinviteEmail(
                "op@tenant.in", "Sunita", "https://app.jalsoochak.in/activate?token=re1", 72);

        verify(smtpMailChannel).send(captor.capture());
        NotificationRequest req = captor.getValue();
        assertThat(req.getSubject()).isEqualTo("Reminder: Your JalSoochak Invitation");
        assertThat(req.getRecipient()).isEqualTo("op@tenant.in");
        assertThat(req.getBody()).contains("Sunita");
        assertThat(req.getBody()).contains("https://app.jalsoochak.in/activate?token=re1");
        assertThat(req.getBody()).contains("72");
        assertThat(req.getBody()).contains("Activate Account");
    }

    @Test
    void sendReinviteEmail_substitutesUserGreeting_whenNameIsNull() {
        ArgumentCaptor<NotificationRequest> captor = ArgumentCaptor.forClass(NotificationRequest.class);

        accountEmailService.sendReinviteEmail("op@tenant.in", null, "https://link", 48);

        verify(smtpMailChannel).send(captor.capture());
        assertThat(captor.getValue().getBody()).contains("Dear User,");
    }

    @Test
    void sendReinviteEmail_throwsRuntimeException_whenSmtpChannelReturnsFalse() {
        when(smtpMailChannel.send(any())).thenReturn(false);

        assertThatThrownBy(() -> accountEmailService.sendReinviteEmail(
                "op@tenant.in", "Dev", "https://link", 24))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to send email");
    }

    @Test
    void sendReinviteEmail_escapesHtmlInNameAndLink() {
        ArgumentCaptor<NotificationRequest> captor = ArgumentCaptor.forClass(NotificationRequest.class);

        accountEmailService.sendReinviteEmail(
                "op@tenant.in", "<b>User</b>",
                "https://app.jalsoochak.in/activate?token=re&next=<evil>", 48);

        verify(smtpMailChannel).send(captor.capture());
        assertThat(captor.getValue().getBody()).doesNotContain("<b>User</b>").doesNotContain("<evil>");
        assertThat(captor.getValue().getBody()).contains("&lt;b&gt;User&lt;/b&gt;");
        assertThat(captor.getValue().getBody()).contains("&amp;next=&lt;evil&gt;");
    }

    // ──────────────────────── sendPasswordResetEmail ───────────────────────────

    @Test
    void sendPasswordResetEmail_setsCorrectSubjectAndContainsLink() {
        ArgumentCaptor<NotificationRequest> captor = ArgumentCaptor.forClass(NotificationRequest.class);

        accountEmailService.sendPasswordResetEmail(
                "user@example.com", "https://app.jalsoochak.in/reset?token=r1", 30);

        verify(smtpMailChannel).send(captor.capture());
        NotificationRequest req = captor.getValue();
        assertThat(req.getSubject()).isEqualTo("Reset Your JalSoochak Password");
        assertThat(req.getRecipient()).isEqualTo("user@example.com");
        assertThat(req.getBody()).contains("https://app.jalsoochak.in/reset?token=r1");
        assertThat(req.getBody()).contains("30");
        assertThat(req.getBody()).contains("Reset Password");
    }

    @Test
    void sendPasswordResetEmail_alwaysUsesDearUserGreeting() {
        // Password reset does not take a name parameter — greeting is always "Dear User"
        ArgumentCaptor<NotificationRequest> captor = ArgumentCaptor.forClass(NotificationRequest.class);

        accountEmailService.sendPasswordResetEmail("user@example.com", "https://link", 15);

        verify(smtpMailChannel).send(captor.capture());
        assertThat(captor.getValue().getBody()).contains("Dear User,");
    }

    @Test
    void sendPasswordResetEmail_throwsRuntimeException_whenSmtpChannelReturnsFalse() {
        when(smtpMailChannel.send(any())).thenReturn(false);

        assertThatThrownBy(() -> accountEmailService.sendPasswordResetEmail(
                "user@example.com", "https://link", 30))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to send email");
    }

    @Test
    void sendPasswordResetEmail_escapesHtmlInResetLink() {
        ArgumentCaptor<NotificationRequest> captor = ArgumentCaptor.forClass(NotificationRequest.class);

        accountEmailService.sendPasswordResetEmail(
                "user@example.com", "https://app.jalsoochak.in/reset?token=r1&next=<evil>", 30);

        verify(smtpMailChannel).send(captor.capture());
        assertThat(captor.getValue().getBody()).doesNotContain("<evil>");
        assertThat(captor.getValue().getBody()).contains("&amp;next=&lt;evil&gt;");
    }
}
