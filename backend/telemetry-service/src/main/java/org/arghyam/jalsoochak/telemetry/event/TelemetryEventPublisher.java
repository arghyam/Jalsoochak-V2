package org.arghyam.jalsoochak.telemetry.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.arghyam.jalsoochak.telemetry.dto.event.WaterQuantityEvent;
import org.arghyam.jalsoochak.telemetry.kafka.KafkaProducer;
import org.arghyam.jalsoochak.telemetry.service.AnomalyConstants;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
@Slf4j
public class TelemetryEventPublisher {

    public static final String TOPIC = "telemetry-service-topic";
    public static final String EVENT_WATER_QUANTITY_RECORDED = "WATER_QUANTITY_RECORDED";
    public static final int NOT_SUBMITTED_STATUS = 0;

    private final KafkaProducer kafkaProducer;

    public void publishOutageOrNonSubmissionReason(Integer tenantId,
                                                   Long schemeId,
                                                   Long userId,
                                                   LocalDate date,
                                                   int anomalyType) {
        ReasonPayload payload = mapReason(anomalyType);
        if (payload == null) {
            return;
        }

        WaterQuantityEvent event = WaterQuantityEvent.builder()
                .eventType(EVENT_WATER_QUANTITY_RECORDED)
                .tenantId(tenantId)
                .schemeId(toInt(schemeId))
                .userId(toInt(userId))
                .waterQuantity(0)
                .submissionStatus(NOT_SUBMITTED_STATUS)
                .outageReason(payload.outageReason)
                .nonSubmissionReason(payload.nonSubmissionReason)
                .date((date != null ? date : LocalDate.now()).toString())
                .build();

        boolean ok = kafkaProducer.publishJson(TOPIC, event);
        if (!ok) {
            log.warn("[telemetry-events] publish_failed type={} tenantId={} schemeId={} userId={}",
                    anomalyType, tenantId, schemeId, userId);
        }
    }

    private static ReasonPayload mapReason(int anomalyType) {
        if (anomalyType == AnomalyConstants.TYPE_NO_WATER_SUPPLY) {
            return new ReasonPayload("TYPE_NO_WATER_SUPPLY", null);
        }
        if (anomalyType == AnomalyConstants.TYPE_LOW_WATER_SUPPLY) {
            return new ReasonPayload("TYPE_LOW_WATER_SUPPLY", null);
        }
        if (anomalyType == AnomalyConstants.TYPE_NO_SUBMISSION) {
            return new ReasonPayload(null, "TYPE_NO_SUBMISSION");
        }
        return null;
    }

    private static Integer toInt(Long value) {
        return value == null ? null : value.intValue();
    }

    private record ReasonPayload(String outageReason, String nonSubmissionReason) {
    }
}
