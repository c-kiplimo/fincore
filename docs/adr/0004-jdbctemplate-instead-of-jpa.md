# ADR-0004: JdbcTemplate instead of JPA

**Status:** Accepted  
**Date:** 2026-10-01

## Context

Ledger correctness depends on exact SQL: row locks in a fixed order, `on conflict` idempotency, append-only tables, aggregate reports.

## Decision

Use `NamedParameterJdbcTemplate` in every service, with SQL written by hand and Flyway migrations.

## Consequences

No hidden queries or lazy loading; the SQL is the design and is easy to review. More code for simple CRUD.
