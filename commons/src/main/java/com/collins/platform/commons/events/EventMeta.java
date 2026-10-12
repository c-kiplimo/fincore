package com.collins.platform.commons.events;

import java.time.Instant;

/** Metadata carried by every domain event. */
public record EventMeta(String eventId, String eventType, Instant occurredAt, String source, String correlationId) {
}
