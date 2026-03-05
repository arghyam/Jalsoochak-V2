package org.arghyam.jalsoochak.tenant.event;

import java.util.HashMap;
import java.util.Map;

import org.arghyam.jalsoochak.tenant.dto.response.TenantResponseDTO;
import org.arghyam.jalsoochak.tenant.kafka.KafkaProducer;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class TenantEventListener {

    private static final String TENANT_TOPIC = "tenant-service-topic";

    private final KafkaProducer kafkaProducer;
    private final StringRedisTemplate redisTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleTenantCreated(TenantCreatedEvent event) {
        log.info("Handling TenantCreatedEvent after commit [id={}]", event.getTenant().getId());
        try {
            cacheTenantInRedis(event.getTenant(), event.getSchemaName());
        } catch (Exception e) {
            log.error("Failed to cache tenant in Redis after create [id={}]", event.getTenant().getId(), e);
        }
        try {
            publishTenantEvent(event.getTenant(), "TENANT_CREATED");
        } catch (Exception e) {
            log.error("Failed to publish TENANT_CREATED event [id={}]", event.getTenant().getId(), e);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleTenantUpdated(TenantUpdatedEvent event) {
        log.info("Handling TenantUpdatedEvent after commit [id={}]", event.getTenant().getId());
        try {
            refreshTenantInRedis(event.getTenant());
        } catch (Exception e) {
            log.error("Failed to refresh tenant in Redis after update [id={}]", event.getTenant().getId(), e);
        }
        try {
            publishTenantEvent(event.getTenant(), "TENANT_UPDATED");
        } catch (Exception e) {
            log.error("Failed to publish TENANT_UPDATED event [id={}]", event.getTenant().getId(), e);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleTenantDeactivated(TenantDeactivatedEvent event) {
        log.info("Handling TenantDeactivatedEvent after commit [id={}]", event.getTenant().getId());
        try {
            evictTenantFromRedis(event.getTenant());
        } catch (Exception e) {
            log.error("Failed to evict tenant from Redis after deactivate [id={}]", event.getTenant().getId(), e);
        }
        try {
            publishTenantEvent(event.getTenant(), "TENANT_DEACTIVATED");
        } catch (Exception e) {
            log.error("Failed to publish TENANT_DEACTIVATED event [id={}]", event.getTenant().getId(), e);
        }
    }

    private void publishTenantEvent(TenantResponseDTO tenant, String eventType) {
        int statusInt = "ACTIVE".equalsIgnoreCase(tenant.getStatus()) ? 1 : 0;
        Map<String, Object> event = Map.of(
                "eventType", eventType,
                "tenantId", tenant.getId(),
                "stateCode", tenant.getStateCode(),
                "title", tenant.getName(),
                "countryCode", "IN",
                "status", statusInt);

        kafkaProducer.publishJson(TENANT_TOPIC, event);
        log.info("Published {} event for tenant [id={}]", eventType, tenant.getId());
    }

    private void cacheTenantInRedis(TenantResponseDTO tenant, String schemaName) {
        String tenantStateCode = tenant.getStateCode().toUpperCase();
        String tenantKey = "tenant-service:tenants:" + tenantStateCode + ":profile";

        Map<String, String> tenantPayload = new HashMap<>();
        tenantPayload.put("id", String.valueOf(tenant.getId()));
        tenantPayload.put("stateCode", tenantStateCode);
        tenantPayload.put("name", tenant.getName());
        tenantPayload.put("status", tenant.getStatus());
        tenantPayload.put("schemaName", schemaName);

        redisTemplate.opsForHash().putAll(tenantKey, tenantPayload);
        redisTemplate.opsForSet().add("tenant-service:tenants:index", tenantStateCode);
        log.info("Tenant cached in Redis under key: {}", tenantKey);
    }

    private void refreshTenantInRedis(TenantResponseDTO tenant) {
        String tenantStateCode = tenant.getStateCode().toUpperCase();
        String tenantKey = "tenant-service:tenants:" + tenantStateCode + ":profile";

        Map<String, String> updates = new HashMap<>();
        updates.put("name", tenant.getName());
        updates.put("status", tenant.getStatus());

        redisTemplate.opsForHash().putAll(tenantKey, updates);
        log.info("Tenant cache refreshed in Redis under key: {}", tenantKey);
    }

    private void evictTenantFromRedis(TenantResponseDTO tenant) {
        String tenantStateCode = tenant.getStateCode().toUpperCase();
        String tenantKey = "tenant-service:tenants:" + tenantStateCode + ":profile";

        redisTemplate.delete(tenantKey);
        redisTemplate.opsForSet().remove("tenant-service:tenants:index", tenantStateCode);
        log.info("Tenant evicted from Redis cache under key: {}", tenantKey);
    }
}
