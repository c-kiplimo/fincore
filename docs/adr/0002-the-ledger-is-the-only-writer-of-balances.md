# ADR-0002: The ledger is the only writer of balances

**Status:** Accepted  
**Date:** 2026-10-01

## Context

Balances spread across services drift, and drift in a money system is a loss nobody can explain.

## Decision

Only `ledger-service` changes balances. Other services keep business state (payment status, loan schedule) and post balanced transactions; they read balances from the ledger.

## Consequences

One source of truth and one audit trail. The ledger is on the critical path for every money movement, so it must be fast and highly available (see 0003).
