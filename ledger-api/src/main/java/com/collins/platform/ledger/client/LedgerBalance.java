package com.collins.platform.ledger.client;

public record LedgerBalance(String entityId, String account, String currency,
                            long postedMinor, long pendingMinor, long availableMinor) {
}
