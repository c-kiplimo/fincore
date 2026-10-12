package com.collins.platform.commons.money;

import java.util.Objects;

/**
 * A value object for money: a whole number of minor units plus an ISO currency code.
 * There is deliberately no double anywhere near money.
 */
public record Money(long minor, String currency) {

    public Money {
        Objects.requireNonNull(currency, "currency");
        if (!CurrencyUnits.supported(currency)) {
            throw new IllegalArgumentException("Unsupported currency " + currency);
        }
    }

    public static Money of(long minor, String currency) {
        return new Money(minor, currency);
    }

    public static Money zero(String currency) {
        return new Money(0, currency);
    }

    public Money plus(Money other) {
        requireSameCurrency(other);
        return new Money(Math.addExact(minor, other.minor), currency);
    }

    public Money minus(Money other) {
        requireSameCurrency(other);
        return new Money(Math.subtractExact(minor, other.minor), currency);
    }

    public Money negate() {
        return new Money(Math.negateExact(minor), currency);
    }

    public boolean isNegative() {
        return minor < 0;
    }

    public boolean isZero() {
        return minor == 0;
    }

    private void requireSameCurrency(Money other) {
        if (!currency.equals(other.currency)) {
            throw new IllegalArgumentException("Currency mismatch: " + currency + " vs " + other.currency);
        }
    }

    @Override
    public String toString() {
        return CurrencyUnits.toMajor(minor, currency).toPlainString() + " " + currency;
    }
}
