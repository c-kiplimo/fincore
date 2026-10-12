# ADR-0005: Money as integer minor units

**Status:** Accepted  
**Date:** 2026-10-01

## Context

Floating point cannot represent 0.10 exactly; rounding errors in money systems accumulate.

## Decision

Store and transmit money as `long` minor units with an ISO 4217 currency; use `BigDecimal` only for rates and convert with half-even rounding. Currency decimals come from a table (JPY 0, KES 2, KWD 3).

## Consequences

Exact arithmetic and simple storage. Every API consumer must remember amounts are in minor units.
