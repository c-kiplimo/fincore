package com.collins.platform.payments.domain;

import java.time.Instant;

public record PaymentIntent(
        String id, String entityId, String merchantId, long amountMinor, String currency, String method, String status,
        String networkRef, String declineReason, long capturedMinor, long refundedMinor, long feeMinor, long taxMinor,
        long netMinor, String quoteId, String description, Instant createdAt, Instant updatedAt) {

    public PaymentStatus state() {
        return PaymentStatus.valueOf(status);
    }

    public long refundableMinor() {
        return capturedMinor - refundedMinor;
    }
}
