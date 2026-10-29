package com.collins.platform.commons.money;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

/**
 * How many decimal places (minor units) each currency has, per ISO 4217.
 * Never assume 2: JPY has 0 and KWD has 3.
 */
public final class CurrencyUnits {

    private static final Map<String, Integer> DIGITS = Map.ofEntries(
            Map.entry("KES", 2), Map.entry("UGX", 0), Map.entry("TZS", 2), Map.entry("NGN", 2),
            Map.entry("ZAR", 2), Map.entry("USD", 2), Map.entry("EUR", 2), Map.entry("GBP", 2),
            Map.entry("JPY", 0), Map.entry("KWD", 3), Map.entry("BHD", 3), Map.entry("INR", 2),
            Map.entry("AAA", 2), Map.entry("BBB", 2), Map.entry("CCC", 3), Map.entry("DDD", 0));

    private CurrencyUnits() {
    }

    public static int digits(String currency) {
        Integer d = DIGITS.get(currency);
        if (d == null) {
            throw new IllegalArgumentException("Unsupported currency " + currency);
        }
        return d;
    }

    public static boolean supported(String currency) {
        return DIGITS.containsKey(currency);
    }

    /** 1234 minor units of KES -> 12.34 */
    public static BigDecimal toMajor(long minor, String currency) {
        return BigDecimal.valueOf(minor).movePointLeft(digits(currency));
    }

    /** 12.345 KES -> 1234 (half-even rounding, also called banker's rounding) */
    public static long toMinor(BigDecimal major, String currency) {
        return major.setScale(digits(currency), RoundingMode.HALF_EVEN).movePointRight(digits(currency)).longValueExact();
    }

    /** Applies a rate (e.g. 0.15 or a FX rate) to a minor amount and rounds half-even to whole minor units. */
    public static long applyRate(long minor, BigDecimal rate) {
        return BigDecimal.valueOf(minor).multiply(rate).setScale(0, RoundingMode.HALF_EVEN).longValueExact();
    }

    /** Converts minor units of one currency to minor units of another at a major-unit rate. */
    public static long convert(long minor, String from, BigDecimal rate, String to) {
        BigDecimal major = toMajor(minor, from).multiply(rate);
        return toMinor(major, to);
    }
}
