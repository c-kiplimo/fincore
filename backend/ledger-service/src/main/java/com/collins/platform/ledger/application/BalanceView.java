package com.collins.platform.ledger.application;

/** Natural balances (positive = the account's normal side). available = posted + pending. */
public record BalanceView(String entityId, String account, String currency, long postedMinor, long pendingMinor, long availableMinor) {
}
