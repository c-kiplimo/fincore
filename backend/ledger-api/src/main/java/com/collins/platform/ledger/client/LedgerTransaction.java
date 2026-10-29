package com.collins.platform.ledger.client;

public record LedgerTransaction(
    String transactionId,
    String entityId,
    String status,
    String transactionType) {
}
