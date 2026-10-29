package com.collins.platform.commons.outbox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Sends unpublished outbox rows to Kafka. {@code FOR UPDATE SKIP LOCKED} lets several replicas
 * run the relay at once without sending the same row twice in the same moment. Consumers must
 * still be idempotent: a crash between send and update can resend a message.
 */
@Component
@ConditionalOnProperty(name = "platform.outbox.relay-enabled", havingValue = "true", matchIfMissing = true)
public class OutboxRelay {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelay.class);

    private final NamedParameterJdbcTemplate jdbc;
    private final KafkaTemplate<String, String> kafka;
    private final TransactionTemplate tx;

    public OutboxRelay(NamedParameterJdbcTemplate jdbc, KafkaTemplate<String, String> kafka, TransactionTemplate tx) {
        this.jdbc = jdbc;
        this.kafka = kafka;
        this.tx = tx;
    }

    @Scheduled(fixedDelayString = "${platform.outbox.relay-interval-ms:1000}")
    public void relay() {
        tx.executeWithoutResult(status -> {
            List<Map<String, Object>> rows = jdbc.queryForList("""
                    select id, topic, msg_key, payload_json from outbox_events
                    where published_at is null order by created_at limit 100
                    for update skip locked
                    """, Map.of());
            for (Map<String, Object> row : rows) {
                try {
                    kafka.send((String) row.get("topic"), (String) row.get("msg_key"), (String) row.get("payload_json"))
                            .get(10, TimeUnit.SECONDS);
                    jdbc.update("update outbox_events set published_at = now() where id = :id", Map.of("id", row.get("id")));
                } catch (Exception e) {
                    log.warn("Outbox relay could not publish event {}; will retry: {}", row.get("id"), e.getMessage());
                    return;
                }
            }
        });
    }
}
