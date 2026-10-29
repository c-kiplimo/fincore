package com.collins.platform.payments.domain;

import com.collins.platform.commons.exception.BusinessRuleException;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * State machine for a payment intent (State pattern as an enum). Illegal moves throw.
 * <pre>
 * AUTHORIZED -> CAPTURED -> PARTIALLY_REFUNDED -> REFUNDED
 *            -> CANCELED          \-> DISPUTED -> CAPTURED (won) / CHARGED_BACK (lost)
 * DECLINED (final)
 * </pre>
 */
public enum PaymentStatus {
    AUTHORIZED, DECLINED, CANCELED, CAPTURED, PARTIALLY_REFUNDED, REFUNDED, DISPUTED, CHARGED_BACK;

    private static final Map<PaymentStatus, Set<PaymentStatus>> ALLOWED = Map.of(
            AUTHORIZED, EnumSet.of(CAPTURED, CANCELED),
            CAPTURED, EnumSet.of(PARTIALLY_REFUNDED, REFUNDED, DISPUTED),
            PARTIALLY_REFUNDED, EnumSet.of(PARTIALLY_REFUNDED, REFUNDED, DISPUTED),
            DISPUTED, EnumSet.of(CAPTURED, PARTIALLY_REFUNDED, CHARGED_BACK),
            DECLINED, EnumSet.noneOf(PaymentStatus.class),
            CANCELED, EnumSet.noneOf(PaymentStatus.class),
            REFUNDED, EnumSet.noneOf(PaymentStatus.class),
            CHARGED_BACK, EnumSet.noneOf(PaymentStatus.class));

    public PaymentStatus moveTo(PaymentStatus next) {
        if (!ALLOWED.get(this).contains(next)) {
            throw new BusinessRuleException("INVALID_PAYMENT_STATE", "Cannot move a payment from " + this + " to " + next);
        }
        return next;
    }
}
