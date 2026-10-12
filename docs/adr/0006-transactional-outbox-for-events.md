# ADR-0006: Transactional outbox for events

**Status:** Accepted  
**Date:** 2026-10-01

## Context

Publishing to Kafka after a database commit can lose events; publishing before can announce changes that roll back.

## Decision

Write the event to `outbox_events` in the same transaction as the change; `OutboxRelay` publishes it. Consumers record processed event ids (`processed_events`).

## Consequences

At-least-once delivery with idempotent consumers. A small delay between commit and publish.
