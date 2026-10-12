# ADR-0009: AI may explain, never post

**Status:** Accepted  
**Date:** 2026-10-01

## Context

An AI helper can speed up reconciliation, but a wrong posting would corrupt the books.

## Decision

The Spring AI explainer in `reconciliation-service` only reads ledger data and returns an explanation and a suggestion. Posting to suspense or resolving is a separate, human-triggered API call. Without a configured model, deterministic rules explain the mismatch.

## Consequences

Safe use of AI with a clear audit boundary. Explanations may be wrong; they are advice.
