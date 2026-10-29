package com.collins.platform.pricing.domain;

import com.collins.platform.commons.money.CurrencyUnits;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/** Fee, tax and FX arithmetic. Pure functions, so they are easy to test. */
public final class PricingRules {

    private PricingRules() {
    }

    /** fee = amount x percent + fixed, kept between min and max. */
    public static long fee(long amountMinor, int percentBp, long fixedMinor, long minMinor, Long maxMinor) {
        long percentPart = BigDecimal.valueOf(amountMinor).multiply(BigDecimal.valueOf(percentBp))
                .divide(BigDecimal.valueOf(10_000), 0, RoundingMode.HALF_EVEN).longValueExact();
        long fee = Math.max(minMinor, percentPart + fixedMinor);
        return maxMinor == null ? fee : Math.min(fee, maxMinor);
    }

    /** Tax is charged on the fee (the platform's service), never on the money being moved. */
    public static long tax(long baseMinor, BigDecimal rate) {
        return CurrencyUnits.applyRate(baseMinor, rate);
    }

    /** The customer's rate is the mid-market rate less the spread; the difference is the platform's FX revenue. */
    public static BigDecimal customerRate(BigDecimal midRate, int spreadBp) {
        BigDecimal factor = BigDecimal.ONE.subtract(BigDecimal.valueOf(spreadBp).divide(BigDecimal.valueOf(10_000), MathContext.DECIMAL64));
        return midRate.multiply(factor, MathContext.DECIMAL64).setScale(10, RoundingMode.HALF_EVEN);
    }
}
