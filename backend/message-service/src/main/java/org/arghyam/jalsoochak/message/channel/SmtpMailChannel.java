package org.arghyam.jalsoochak.message.channel;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.arghyam.jalsoochak.message.dto.NotificationRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

/**
 * {@link NotificationChannel} implementation that delivers email via SMTP
 * using Spring's {@link JavaMailSender}.
 *
 * <p>The {@code body} field of {@link NotificationRequest} is treated as HTML content.</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SmtpMailChannel implements NotificationChannel {

    public static final String CHANNEL_TYPE = "SMTP_EMAIL";

    private final JavaMailSender mailSender;

    @Value("${notification.channel.smtp.from-address}")
    private String fromAddress;

    @Value("${notification.channel.smtp.from-name}")
    private String fromName;

    @Override
    public String channelType() {
        return CHANNEL_TYPE;
    }

    @Override
    public boolean send(NotificationRequest request) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromAddress, fromName);
            helper.setTo(request.getRecipient());
            helper.setSubject(request.getSubject());
            helper.setText(request.getBody(), true); // body is HTML
            mailSender.send(message);
            log.info("[SmtpMailChannel] Email sent to {}", maskEmail(request.getRecipient()));
            return true;
        } catch (Exception e) {
            log.error("[SmtpMailChannel] Failed to send email to {}: {}",
                    maskEmail(request.getRecipient()), e.getMessage(), e);
            return false;
        }
    }

    private static String maskEmail(String email) {
        if (email == null) return null;
        int at = email.indexOf('@');
        if (at <= 0) return "***";
        return email.charAt(0) + "***" + email.substring(at);
    }
}
