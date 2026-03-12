package org.arghyam.jalsoochak.user.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.arghyam.jalsoochak.user.kafka.KafkaProducer;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.concurrent.CompletableFuture;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserEventPublisher {

    public static final String COMMON_TOPIC = "common-topic";

    private final KafkaProducer kafkaProducer;

    public void publishUserLanguageUpdatedAfterCommit(List<UserLanguageUpdatedEvent> events) {
        if (events == null || events.isEmpty()) {
            return;
        }

        log.info("[user-events] queued_after_commit count={} topic={}", events.size(), COMMON_TOPIC);

        Runnable publish = () -> {
            for (UserLanguageUpdatedEvent e : events) {
                boolean ok = kafkaProducer.publishJson(COMMON_TOPIC, e);
                if (!ok) {
                    log.warn("Failed to publish user language updated event (best-effort): tenantId={} userId={}",
                            e.getTenantId(), e.getUserId());
                }
            }
        };

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    // Never block the request thread on Kafka metadata/broker availability.
                    CompletableFuture.runAsync(publish)
                            .exceptionally(ex -> {
                                log.error("Async user event publish failed (best-effort): {}", ex.getMessage(), ex);
                                return null;
                            });
                }
            });
        } else {
            log.warn("No active transaction synchronization; publishing user events immediately");
            CompletableFuture.runAsync(publish)
                    .exceptionally(ex -> {
                        log.error("Async user event publish failed (best-effort): {}", ex.getMessage(), ex);
                        return null;
                    });
        }
    }
}
