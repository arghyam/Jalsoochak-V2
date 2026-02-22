package com.example.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;

@Service
@Slf4j
@RequiredArgsConstructor
public class MailService {

    private final JavaMailSender mailSender;

    @Value("${mail.from.email}")
    private String fromEmail;

    @Value("${mail.from.name}")
    private String fromName;


    public void sendInviteMail(String recipientEmail, String inviteLink) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail, fromName);
            helper.setTo(recipientEmail);
            helper.setSubject("You're invited to join the Jalsoochak platform");

            String htmlContent = """
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
                    """.formatted(inviteLink);

            helper.setText("Hello,\n\nYou have been invited to join the Jalsoochak platform.\n"
                            + "Click the link to set your password: " + inviteLink + "\n\n"
                            + "This link expires in 24 hours.",
                    htmlContent);

            mailSender.send(message);

            log.info("Invite email sent successfully to {}", recipientEmail);

        } catch (Exception e) {
            log.error("Failed to send invite email to {}", recipientEmail, e);
            throw new RuntimeException("Failed to send invite email", e);
        }
    }
}
