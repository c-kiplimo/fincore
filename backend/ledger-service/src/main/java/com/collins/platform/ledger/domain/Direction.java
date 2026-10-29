package com.collins.platform.ledger.domain;

/** Debit = left side of the accounting equation; credit = right side. */
public enum Direction {
    DEBIT, CREDIT;

    public Direction opposite() {
        return this == DEBIT ? CREDIT : DEBIT;
    }

    /** Signed effect on a debit-positive net balance. */
    public long signed(long amountMinor) {
        return this == DEBIT ? amountMinor : -amountMinor;
    }
}
