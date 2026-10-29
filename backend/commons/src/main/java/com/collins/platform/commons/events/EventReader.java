package com.collins.platform.commons.events;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

/** Parses Kafka messages into {@link EventEnvelope}s and payload records. */
@Component
public class EventReader {

    private final ObjectMapper mapper;

    public EventReader(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public EventEnvelope read(String message) {
        try {
            return mapper.readValue(message, EventEnvelope.class);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Malformed event: " + e.getMessage(), e);
        }
    }

    public <T> T payload(EventEnvelope envelope, Class<T> type) {
        try {
            return mapper.treeToValue(envelope.payload(), type);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Malformed event payload: " + e.getMessage(), e);
        }
    }
}
