# ADR-0007: Sagas instead of distributed transactions

**Status:** Accepted  
**Date:** 2026-10-01

## Context

A payout touches the ledger and an external bank; a recovery touches several accounts. Two-phase commit across services and banks is not available or wise.

## Decision

Use orchestrated sagas with compensations: payout hold (pending) → bank → commit or void; loan disbursement; recovery steps. Each step is idempotent.

## Consequences

Failures are visible states (SENT, FAILED) instead of hidden locks. Retry jobs are needed for unknown outcomes.
