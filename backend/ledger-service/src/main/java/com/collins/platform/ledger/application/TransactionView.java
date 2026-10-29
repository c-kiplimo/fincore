package com.collins.platform.ledger.application;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** A journal transaction as returned by the API. */
public record TransactionView(
        String transactionId,
        String entityId,
        String status,
        String transactionType,
        String description,
        LocalDate effectiveDate,
        String referenceType,
        String referenceId,
        String reversalOf,
        Instant createdAt,
        List<Line> entries) {

    public record Line(int lineNo, String account, String direction, long amountMinor, String currency) {
    }
}
