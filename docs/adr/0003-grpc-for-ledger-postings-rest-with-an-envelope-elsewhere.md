# ADR-0003: gRPC for ledger postings, REST with an envelope elsewhere

**Status:** Accepted  
**Date:** 2026-10-01

## Context

Every money movement calls the ledger, so that call is the busiest internal path. Public APIs must be easy to use and debug.

## Decision

Expose `ledger.v1` over gRPC (binary, HTTP/2, typed contract in `ledger-api`). Use REST with the `{payload}` / `{meta, payload}` envelope for public APIs and other internal calls. The gRPC messages follow the same meta/payload shape.

## Consequences

Faster, contract-checked postings; two protocols to operate. `buf breaking` (or protoc compatibility checks) should guard the contract in CI.
