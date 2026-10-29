package com.collins.platform.payouts;

import java.time.Instant;

public record Payout(String id, String entityId, String beneficiaryType, String beneficiaryId, String sourceAccount,
                     long amountMinor, String currency, long feeMinor, long taxMinor, long totalDebitedMinor,
                     String destinationType, String destinationAccount, String status, String pendingTxnId,
                     String confirmTxnId, String bankRef, String failureReason, int attempts, Instant createdAt) {
}
