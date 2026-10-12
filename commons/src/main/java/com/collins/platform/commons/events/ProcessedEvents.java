package com.collins.platform.commons.events;

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Idempotent consumer support. Kafka delivers at least once, so every consumer records the
 * event ids it has handled and skips repeats. Needs the {@code processed_events} table.
 */
@Component
public class ProcessedEvents {

    private final NamedParameterJdbcTemplate jdbc;

    public ProcessedEvents(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** @return true the first time this consumer sees the event, false for a duplicate. */
    public boolean markProcessed(String consumer, String eventId) {
        return jdbc.update("""
                insert into processed_events(consumer, event_id) values (:consumer, :eventId)
                on conflict (consumer, event_id) do nothing
                """, Map.of("consumer", consumer, "eventId", eventId)) == 1;
    }
}
