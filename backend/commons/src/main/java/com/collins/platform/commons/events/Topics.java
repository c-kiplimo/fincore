package com.collins.platform.commons.events;

/** Kafka topic names: {context}.{event}.v{version}. */
public final class Topics {

    public static final String PAYMENT_CAPTURED = "payments.payment-captured.v1";
    public static final String PAYMENT_REFUNDED = "payments.payment-refunded.v1";
    public static final String DISPUTE_OPENED = "payments.dispute-opened.v1";
    public static final String PAYOUT_COMPLETED = "payouts.payout-completed.v1";
    public static final String PAYOUT_FAILED = "payouts.payout-failed.v1";
    public static final String LOAN_DISBURSED = "lending.loan-disbursed.v1";
    public static final String LOAN_INSTALMENT_MISSED = "lending.loan-instalment-missed.v1";
    public static final String LEDGER_TRANSACTION_POSTED = "ledger.transaction-posted.v1";

    private Topics() {
    }
}
