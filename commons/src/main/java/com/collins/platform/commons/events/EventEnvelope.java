package com.collins.platform.commons.events;

import com.fasterxml.jackson.databind.JsonNode;

/** Kafka messages use the same meta/payload idea as the HTTP API. */
public record EventEnvelope(EventMeta meta, JsonNode payload) {
}
