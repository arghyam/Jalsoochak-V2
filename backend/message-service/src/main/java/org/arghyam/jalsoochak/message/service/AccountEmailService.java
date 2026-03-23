package org.arghyam.jalsoochak.message.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.arghyam.jalsoochak.message.channel.SmtpMailChannel;
import org.arghyam.jalsoochak.message.dto.NotificationRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

/**
 * Builds and dispatches account lifecycle emails (invite, reinvite, password reset)
 * via {@link SmtpMailChannel}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AccountEmailService {

    // ── HTML wrapper ──────────────────────────────────────────────────────────

    private static final String EMAIL_WRAPPER = """
            <!DOCTYPE html>
            <html lang="en">
            <head>
              <meta charset="UTF-8"/>
              <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
              <title>%s</title>
            </head>
            <body style="margin:0;padding:0;background-color:#f4f6f9;font-family:Arial,Helvetica,sans-serif;">
              <table width="100%%" cellpadding="0" cellspacing="0" style="background-color:#f4f6f9;padding:40px 20px;">
                <tr>
                  <td align="center">
                    <table width="600" cellpadding="0" cellspacing="0" style="max-width:600px;width:100%%;">

                      <!-- Header -->
                      <tr>
                        <td style="background-color:#1a5276;padding:28px 40px;border-radius:8px 8px 0 0;text-align:center;">
                          <p style="margin:0;font-size:22px;font-weight:bold;color:#ffffff;letter-spacing:1px;">JalSoochak</p>
                          <p style="margin:6px 0 0;font-size:12px;color:#aed6f1;letter-spacing:0.5px;">National Water Quality Monitoring Platform</p>
                        </td>
                      </tr>

                      <!-- Body -->
                      <tr>
                        <td style="background-color:#ffffff;padding:40px;border-left:1px solid #dce3eb;border-right:1px solid #dce3eb;">
                          %s
                        </td>
                      </tr>

                      <!-- Footer -->
                      <tr>
                        <td style="background-color:#eaf0f6;padding:24px 40px;border-radius:0 0 8px 8px;border:1px solid #dce3eb;border-top:none;text-align:center;">
                          <p style="margin:0;font-size:12px;color:#5d6d7e;">
                            This is an automated message from the JalSoochak platform.<br/>
                            Please do not reply to this email.
                          </p>
                          <p style="margin:10px 0 0;font-size:11px;color:#909090;">
                            Supported by Arghyam Foundation &nbsp;|&nbsp; Ministry of Jal Shakti, Government of India
                          </p>
                        </td>
                      </tr>

                    </table>
                  </td>
                </tr>
              </table>
            </body>
            </html>
            """;

    // ── Body templates ────────────────────────────────────────────────────────

    /** Params: greeting, inviteLink, expiryHours, inviteLink (fallback). */
    private static final String INVITE_BODY = """
            <p style="margin:0 0 8px;font-size:15px;color:#1a2533;">Dear %s,</p>
            <p style="margin:0 0 20px;font-size:14px;color:#4a4a4a;line-height:1.6;">
              You have been officially invited to join the <strong>JalSoochak</strong> platform —
              a national initiative for real-time water quality monitoring and reporting.
            </p>
            <p style="margin:0 0 20px;font-size:14px;color:#4a4a4a;line-height:1.6;">
              To activate your account, please set a password using the link below:
            </p>
            <p style="margin:24px 0;text-align:center;">
              <a href="%s"
                 style="display:inline-block;background-color:#1a5276;color:#ffffff;
                        font-size:14px;font-weight:bold;text-decoration:none;
                        padding:13px 32px;border-radius:5px;letter-spacing:0.5px;">
                Activate Account
              </a>
            </p>
            <p style="margin:0 0 6px;font-size:13px;color:#e74c3c;">
              &#9888;&nbsp; This link will expire in <strong>%d hours</strong>.
            </p>
            <p style="margin:0;font-size:13px;color:#7f8c8d;">
              If the button above does not work, copy and paste the following URL into your browser:
            </p>
            <p style="margin:8px 0 0;font-size:12px;color:#2471a3;word-break:break-all;">%s</p>
            """;

    /** Params: greeting, inviteLink, expiryHours, inviteLink (fallback). */
    private static final String REINVITE_BODY = """
            <p style="margin:0 0 8px;font-size:15px;color:#1a2533;">Dear %s,</p>
            <p style="margin:0 0 20px;font-size:14px;color:#4a4a4a;line-height:1.6;">
              This is a reminder that you have a pending invitation to join the
              <strong>JalSoochak</strong> platform. A new activation link has been
              generated for you — please use it to set your password.
            </p>
            <p style="margin:24px 0;text-align:center;">
              <a href="%s"
                 style="display:inline-block;background-color:#1a5276;color:#ffffff;
                        font-size:14px;font-weight:bold;text-decoration:none;
                        padding:13px 32px;border-radius:5px;letter-spacing:0.5px;">
                Activate Account
              </a>
            </p>
            <p style="margin:0 0 6px;font-size:13px;color:#e74c3c;">
              &#9888;&nbsp; This link will expire in <strong>%d hours</strong>.
            </p>
            <p style="margin:0;font-size:13px;color:#7f8c8d;">
              If the button above does not work, copy and paste the following URL into your browser:
            </p>
            <p style="margin:8px 0 0;font-size:12px;color:#2471a3;word-break:break-all;">%s</p>
            """;

    /** Params: resetLink, expiryMinutes, resetLink (fallback). */
    private static final String RESET_PASSWORD_BODY = """
            <p style="margin:0 0 8px;font-size:15px;color:#1a2533;">Dear User,</p>
            <p style="margin:0 0 20px;font-size:14px;color:#4a4a4a;line-height:1.6;">
              We received a request to reset the password associated with your JalSoochak account.
              If you did not initiate this request, please disregard this email — your account will remain secure.
            </p>
            <p style="margin:0 0 8px;font-size:14px;color:#4a4a4a;line-height:1.6;">
              To reset your password, click the button below:
            </p>
            <p style="margin:24px 0;text-align:center;">
              <a href="%s"
                 style="display:inline-block;background-color:#1a5276;color:#ffffff;
                        font-size:14px;font-weight:bold;text-decoration:none;
                        padding:13px 32px;border-radius:5px;letter-spacing:0.5px;">
                Reset Password
              </a>
            </p>
            <p style="margin:0 0 6px;font-size:13px;color:#e74c3c;">
              &#9888;&nbsp; This link will expire in <strong>%d minutes</strong>.
            </p>
            <p style="margin:0;font-size:13px;color:#7f8c8d;">
              If the button above does not work, copy and paste the following URL into your browser:
            </p>
            <p style="margin:8px 0 0;font-size:12px;color:#2471a3;word-break:break-all;">%s</p>
            """;

    // ── Dependencies ──────────────────────────────────────────────────────────

    private final SmtpMailChannel smtpMailChannel;

    // ── Public methods ────────────────────────────────────────────────────────────

    public void sendInviteEmail(String to, String name, String role, String inviteLink, int expiryHours) {
        String subject = resolveInviteSubject(role);
        String safeGreeting = resolveGreeting(name);
        String safeLink = HtmlUtils.htmlEscape(inviteLink);
        String body = EMAIL_WRAPPER.formatted(subject,
                INVITE_BODY.formatted(safeGreeting, safeLink, expiryHours, safeLink));
        dispatch(to, subject, body);
    }

    public void sendReinviteEmail(String to, String name, String inviteLink, int expiryHours) {
        String subject = "Reminder: Your JalSoochak Invitation";
        String safeGreeting = resolveGreeting(name);
        String safeLink = HtmlUtils.htmlEscape(inviteLink);
        String body = EMAIL_WRAPPER.formatted(subject,
                REINVITE_BODY.formatted(safeGreeting, safeLink, expiryHours, safeLink));
        dispatch(to, subject, body);
    }

    public void sendPasswordResetEmail(String to, String resetLink, int expiryMinutes) {
        String subject = "Reset Your JalSoochak Password";
        String safeLink = HtmlUtils.htmlEscape(resetLink);
        String body = EMAIL_WRAPPER.formatted(subject,
                RESET_PASSWORD_BODY.formatted(safeLink, expiryMinutes, safeLink));
        dispatch(to, subject, body);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private void dispatch(String to, String subject, String htmlBody) {
        NotificationRequest request = NotificationRequest.builder()
                .recipient(to)
                .subject(subject)
                .body(htmlBody)
                .channel(SmtpMailChannel.CHANNEL_TYPE)
                .build();
        boolean sent = smtpMailChannel.send(request);
        if (!sent) {
            throw new RuntimeException("Failed to send email via SMTP — see SmtpMailChannel logs for details");
        }
    }

    private static String resolveGreeting(String name) {
        return (name != null && !name.isBlank()) ? HtmlUtils.htmlEscape(name) : "User";
    }

    private static String resolveInviteSubject(String role) {
        if (role == null) return "You are invited to join JalSoochak";
        return switch (role.toUpperCase()) {
            case "STATE_ADMIN" -> "You are assigned as State System Admin by JalSoochak";
            case "SUPER_USER"  -> "You are assigned as Super User by JalSoochak";
            default            -> "You are invited to join JalSoochak";
        };
    }
}
