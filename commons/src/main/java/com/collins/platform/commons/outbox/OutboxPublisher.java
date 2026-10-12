package com.collins.platform.commons.outbox;

import com.collins.platform.commons.events.EventEnvelope;
import com.collins.platform.commons.events.EventMeta;
import com.collins.platform.commons.web.RequestContext;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

/**
 * Transactional outbox: call {@link #publish} inside the same database transaction as the business
 * change. The event is only a row until {@link OutboxRelay} sends it to Kafka, so an event is never
 * published for a change that rolled back, and never lost for a change that committed.
 */
@Component
public class OutboxPublisher {

    private final NamedParameterJdbcTemplate jdbc;
    private final ObjectMapper mapper;
    private final String source;

    public OutboxPublisher(NamedParameterJdbcTemplate jdbc, ObjectMapper mapper,
                           @Value("${spring.application.name:unknown}") String source) {
        this.jdbc = jdbc;
        this.mapper = mapper;
        this.source = source;
    }

    public String publish(String topic, String key, String eventType, Object payload) {
        String eventId = UUID.randomUUID().toString();
        EventEnvelope envelope = new EventEnvelope(
                new EventMeta(eventId, eventType, Instant.now(), source, RequestContext.requestId()),
                mapper.valueToTree(payload));
        try {
            jdbc.update("""
                    insert into outbox_events(id, topic, msg_key, event_type, payload_json)
                    values (cast(:id as uuid), :topic, :key, :type, :json)
                    """, new MapSqlParameterSource()
                    .addValue("id", eventId).addValue("topic", topic).addValue("key", key)
                    .addValue("type", eventType).addValue("json", mapper.writeValueAsString(envelope)));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cannot serialize event", e);
        }
        return eventId;
    }
}
