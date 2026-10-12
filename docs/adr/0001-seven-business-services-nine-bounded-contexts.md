# ADR-0001: Seven business services, nine bounded contexts

**Status:** Accepted  
**Date:** 2026-10-01

## Context

The domain has nine bounded contexts (ledger, payments, pricing, merchants, wallets, payouts, loans, collections, reconciliation). One person builds and runs the system.

## Decision

Deploy seven business services. Merchants and wallets live in `accounts-service`; loans and collections live in `lending-service`. Each module keeps its own package, schema and migration folder and talks to the others only through interfaces.

## Consequences

Fewer deployables to run while every boundary stays visible. Extracting a module later is a mechanical change (and a good follow-up ADR). The cost is that two modules share a deployment and a failure domain.
