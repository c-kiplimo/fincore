package com.collins.platform.ledger.infrastructure;

import java.time.Instant;
import java.time.LocalDate;

/** Row types read by the JDBC repositories. */
public final class LedgerRows {

    private LedgerRows() {
    }

    public record EntityRow(String entityId, String name, String country, String functionalCurrency, String timezone) {
    }

    public record ChartAccount(String entityId, String code, String name, String accountType, String normalSide) {
    }

    public record AccountRef(long id, String entityId, String account, String baseCode, String currency, String normalSide) {
    }

    public record BalanceRow(long accountId, long postedNet, long pendingNet) {
    }

    public record TxnRow(String id, String entityId, String idempotencyKey, String requestHash, String transactionType,
                         String description, LocalDate effectiveDate, String referenceType, String referenceId,
                         String status, String reversalOf, Instant createdAt) {
    }

    public record EntryRow(int lineNo, long accountId, String account, String direction, long amountMinor,
                           String currency, String normalSide) {
    }
}
