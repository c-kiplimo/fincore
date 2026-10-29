package com.collins.platform.payments.api;

/**
 * Idempotency scope constants for payment operations.
 * Scopes namespace idempotency keys to prevent conflicts across different operation types.
 */
public final class IdempotencyScopes {

    private IdempotencyScopes() {
        // Utility class - no instantiation
    }

    /**
     * Scope for creating payment intents.
     * All payment creations share this scope.
     */
    public static final String CREATE_PAYMENT = "create-payment";

    /**
     * Scope prefix for refund operations.
     * Format: "refund:{paymentId}"
     * Each payment's refunds are namespaced separately.
     */
    public static final String REFUND_PREFIX = "refund:";

    /**
     * Scope prefix for dispute operations.
     * Format: "dispute:{paymentId}"
     * Each payment's disputes are namespaced separately.
     */
    public static final String DISPUTE_PREFIX = "dispute:";

    /**
     * Builds refund scope for a specific payment.
     *
     * @param paymentId The payment intent ID
     * @return Scoped key like "refund:pi_123"
     */
    public static String refundScope(String paymentId) {
        return REFUND_PREFIX + paymentId;
    }

    /**
     * Builds dispute scope for a specific payment.
     *
     * @param paymentId The payment intent ID
     * @return Scoped key like "dispute:pi_123"
     */
    public static String disputeScope(String paymentId) {
        return DISPUTE_PREFIX + paymentId;
    }
}