package com.collins.platform.pricing.domain;

import java.math.BigDecimal;
import java.math.MathContext;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/** How days are counted when computing interest. The same loan gives a different interest under each. */
public enum DayCountConvention {
    /** Actual days / 365 (common for consumer loans in Kenya and the UK). */
    ACT_365,
    /** Actual days / 360 (common in US and money markets). */
    ACT_360,
    /** Every month counts as 30 days, the year as 360 (30/360 US simplified). */
    THIRTY_360;

    public BigDecimal yearFraction(LocalDate from, LocalDate to) {
        return switch (this) {
            case ACT_365 -> BigDecimal.valueOf(ChronoUnit.DAYS.between(from, to)).divide(BigDecimal.valueOf(365), MathContext.DECIMAL64);
            case ACT_360 -> BigDecimal.valueOf(ChronoUnit.DAYS.between(from, to)).divide(BigDecimal.valueOf(360), MathContext.DECIMAL64);
            case THIRTY_360 -> {
                int d1 = Math.min(from.getDayOfMonth(), 30);
                int d2 = (to.getDayOfMonth() == 31 && d1 == 30) ? 30 : to.getDayOfMonth();
                long days = 360L * (to.getYear() - from.getYear()) + 30L * (to.getMonthValue() - from.getMonthValue()) + (d2 - d1);
                yield BigDecimal.valueOf(days).divide(BigDecimal.valueOf(360), MathContext.DECIMAL64);
            }
        };
    }
}
