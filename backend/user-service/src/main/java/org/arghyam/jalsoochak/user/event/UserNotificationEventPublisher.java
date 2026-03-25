package org.arghyam.jalsoochak.user.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.arghyam.jalsoochak.user.kafka.KafkaProducer;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Publishes email notification events to the common Kafka topic after the
 * current DB transaction commits, preventing events for rolled-back operations.
 *
 * <p>If no transaction is active (e.g. in tests or async contexts), the event
 * is published immediately.</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class UserNotificationEventPublisher {

    private static final String COMMON_TOPIC = "common-topic";

    private final KafkaProducer kafkaProducer;

    public void publishInviteEmailAfterCommit(InviteEmailEvent event) {
        publishAfterCommit("SEND_INVITE_EMAIL", event);
    }

    public void publishResetPasswordEmailAfterCommit(ResetPasswordEmailEvent event) {
        publishAfterCommit("SEND_PASSWORD_RESET_EMAIL", event);
    }

    public void publishLoginOtpAfterCommit(SendLoginOtpEvent event) {
        publishAfterCommit("SEND_LOGIN_OTP", event);
    }

    private void publishAfterCommit(String label, Object event) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    boolean ok = kafkaProducer.publishJson(COMMON_TOPIC, event);
                    if (!ok) {
                        log.warn("[notification-event] Failed to publish {} event to topic={}", label, COMMON_TOPIC);
                    }
                }
            });
        } else {
            log.warn("[notification-event] No active transaction; publishing {} event immediately", label);
            boolean ok = kafkaProducer.publishJson(COMMON_TOPIC, event);
            if (!ok) {
                log.warn("[notification-event] Failed to publish {} event to topic={}", label, COMMON_TOPIC);
            }
        }
    }
}
