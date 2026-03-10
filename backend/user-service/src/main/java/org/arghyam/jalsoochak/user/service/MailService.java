package org.arghyam.jalsoochak.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
@Slf4j
@RequiredArgsConstructor
public class MailService {

    private static final String RESET_PASSWORD_HTML = """
            <p>Hello,</p>
            <p>We received a request to reset your password.</p>
            <p>Click the button below to reset it:</p>
            <p>
              <a href="%s"
                 style="background:#2563eb;color:white;
                 padding:10px 16px;text-decoration:none;
                 border-radius:6px;">
                Reset Password
              </a>
            </p>
            <p>This link expires in 1 hour.</p>
            """;

    private static final String INVITE_HTML = """
            <p>Hello,</p>
            <p>You have been invited to join the Jalsoochak platform.</p>
            <p>Click the button below to set your password:</p>
            <p>
              <a href="%s"
                 style="background:#2563eb;color:white;
                 padding:10px 16px;text-decoration:none;
                 border-radius:6px;">
                Set Password
              </a>
            </p>
            <p>This link expires in 24 hours.</p>
            """;

    private final JavaMailSender mailSender;

    @Value("${mail.from.email}")
    private String fromEmail;

    @Value("${mail.from.name}")
    private String fromName;


    public void sendPasswordResetMail(String recipientEmail, String resetLink) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail, fromName);
            helper.setTo(recipientEmail);
            helper.setSubject("Reset your JalSoochak password");
            helper.setText("Hello,\n\nClick the link to reset your password: " + resetLink
                    + "\n\nThis link expires in 1 hour.", RESET_PASSWORD_HTML.formatted(resetLink));

            mailSender.send(message);
            log.info("Password reset email sent successfully to {}", recipientEmail);
        } catch (Exception e) {
            log.error("Failed to send password reset email to {}", recipientEmail, e);
            throw new RuntimeException("Failed to send password reset email", e);
        }
    }

    public void sendInviteMail(String recipientEmail, String inviteLink) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail, fromName);
            helper.setTo(recipientEmail);
            helper.setSubject("You're invited to join the Jalsoochak platform");
            helper.setText("Hello,\n\nYou have been invited to join the Jalsoochak platform.\n"
                            + "Click the link to set your password: " + inviteLink
                            + "\n\nThis link expires in 24 hours.",
                    INVITE_HTML.formatted(inviteLink));

            mailSender.send(message);
            log.info("Invite email sent successfully to {}", recipientEmail);
        } catch (Exception e) {
            log.error("Failed to send invite email to {}", recipientEmail, e);
            throw new RuntimeException("Failed to send invite email", e);
        }
    }

    /**
     * Sends email after the current DB transaction commits, or immediately if no
     * transaction is active. Prevents sending emails for rolled-back operations.
     */
    public void sendMailAfterCommit(Runnable mailTask) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    try {
                        mailTask.run();
                    } catch (Exception e) {
                        log.error("Failed to send email after transaction commit", e);
                    }
                }
            });
        } else {
            try {
                mailTask.run();
            } catch (Exception e) {
                log.error("Failed to send email", e);
            }
        }
    }
}
