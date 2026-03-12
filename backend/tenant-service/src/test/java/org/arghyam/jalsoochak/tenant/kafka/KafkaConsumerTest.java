package org.arghyam.jalsoochak.tenant.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.arghyam.jalsoochak.tenant.repository.NudgeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;

import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link KafkaConsumer}.
 *
 * <p>Verifies that {@code WHATSAPP_CONTACT_REGISTERED} events trigger an update
 * to {@code user_table.whatsapp_connection_id} and that other event types are
 * silently ignored.</p>
 */
@ExtendWith(MockitoExtension.class)
class KafkaConsumerTest {

    @Mock
    private NudgeRepository nudgeRepository;

    @Mock
    private Acknowledgment ack;

    private KafkaConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new KafkaConsumer(new ObjectMapper(), nudgeRepository);
    }

    @Test
    void consume_updatesWhatsappConnectionId_forRegisteredEvent() {
        String json = """
                {"eventType":"WHATSAPP_CONTACT_REGISTERED","tenantSchema":"tenant_mp",
                 "userId":42,"contactId":7}
                """;

        consumer.consume(json, ack);

        verify(nudgeRepository).updateWhatsAppConnectionId("tenant_mp", 42L, 7L);
        verify(ack).acknowledge();
    }

    @Test
    void consume_skips_whenSchemaIsBlank() {
        String json = """
                {"eventType":"WHATSAPP_CONTACT_REGISTERED","tenantSchema":"",
                 "userId":42,"contactId":7}
                """;

        consumer.consume(json, ack);

        verify(nudgeRepository, never()).updateWhatsAppConnectionId(anyString(), anyLong(), anyLong());
        verify(ack).acknowledge();
    }

    @Test
    void consume_skips_whenUserIdIsZero() {
        String json = """
                {"eventType":"WHATSAPP_CONTACT_REGISTERED","tenantSchema":"tenant_mp",
                 "userId":0,"contactId":7}
                """;

        consumer.consume(json, ack);

        verify(nudgeRepository, never()).updateWhatsAppConnectionId(anyString(), anyLong(), anyLong());
        verify(ack).acknowledge();
    }

    @Test
    void consume_skips_whenContactIdIsZero() {
        String json = """
                {"eventType":"WHATSAPP_CONTACT_REGISTERED","tenantSchema":"tenant_mp",
                 "userId":42,"contactId":0}
                """;

        consumer.consume(json, ack);

        verify(nudgeRepository, never()).updateWhatsAppConnectionId(anyString(), anyLong(), anyLong());
        verify(ack).acknowledge();
    }

    @Test
    void consume_ignoresOtherEventTypes_silently() {
        String json = """
                {"eventType":"NUDGE","recipientPhone":"919876543210","operatorName":"Op"}
                """;

        consumer.consume(json, ack);

        verify(nudgeRepository, never()).updateWhatsAppConnectionId(anyString(), anyLong(), anyLong());
        verify(ack).acknowledge();
    }

    @Test
    void consume_acknowledgesEvenOnException_bestEffort() {
        doThrow(new IllegalArgumentException("Invalid schema name: bad-schema!"))
                .when(nudgeRepository).updateWhatsAppConnectionId(anyString(), anyLong(), anyLong());

        String json = """
                {"eventType":"WHATSAPP_CONTACT_REGISTERED","tenantSchema":"bad-schema!",
                 "userId":42,"contactId":7}
                """;

        // Should NOT throw — best-effort acknowledge
        consumer.consume(json, ack);

        verify(ack).acknowledge();
    }

    @Test
    void consume_acknowledgesEvenOnMalformedJson() {
        consumer.consume("{not valid json", ack);

        verify(nudgeRepository, never()).updateWhatsAppConnectionId(anyString(), anyLong(), anyLong());
        verify(ack).acknowledge();
    }
}
