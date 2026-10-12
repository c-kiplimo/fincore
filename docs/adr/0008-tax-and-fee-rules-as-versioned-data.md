# ADR-0008: Tax and fee rules as versioned data

**Status:** Accepted  
**Date:** 2026-10-01

## Context

Tax rates change every year (Finance Acts) and differ per country; old transactions must be explainable under the rule of their date.

## Decision

Keep fee plans and tax rules in `pricing-service` tables with validity dates and versions. Every quote stores the rule code and version used.

## Consequences

A tax change is a data change, not a deployment. Rules must be reviewed carefully because they are not in code review.
