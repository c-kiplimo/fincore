package com.collins.platform.pricing.domain;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Interest and amortization formulas. Pure functions; amounts in minor units. */
public final class InterestMath {

    public record ScheduleRow(int number, LocalDate dueDate, long openingPrincipalMinor, long interestMinor,
                              long principalMinor, long instalmentMinor, long closingPrincipalMinor) {
    }

    private InterestMath() {
    }

    /** interest = principal x annual rate x year fraction (day-count convention), rounded half-even. */
    public static long accrue(long principalMinor, int annualRateBp, LocalDate from, LocalDate to, DayCountConvention dayCount) {
        BigDecimal rate = BigDecimal.valueOf(annualRateBp).divide(BigDecimal.valueOf(10_000), MathContext.DECIMAL64);
        return BigDecimal.valueOf(principalMinor).multiply(rate).multiply(dayCount.yearFraction(from, to))
                .setScale(0, RoundingMode.HALF_EVEN).longValueExact();
    }

    /**
     * Annuity schedule: equal monthly instalments, each split into interest on the remaining principal
     * plus principal. Instalment = P * r / (1 - (1 + r)^-n), with r = annual rate / 12.
     * The last instalment absorbs rounding so the loan ends at exactly zero.
     */
    public static List<ScheduleRow> annuity(long principalMinor, int annualRateBp, int months, LocalDate firstDueDate) {
        MathContext mc = MathContext.DECIMAL64;
        BigDecimal r = BigDecimal.valueOf(annualRateBp).divide(BigDecimal.valueOf(120_000), mc); // bp -> monthly fraction
        BigDecimal p = BigDecimal.valueOf(principalMinor);
        long instalment;
        if (r.signum() == 0) {
            instalment = BigDecimal.valueOf(principalMinor).divide(BigDecimal.valueOf(months), 0, RoundingMode.HALF_EVEN).longValueExact();
        } else {
            BigDecimal denominator = BigDecimal.ONE.subtract(BigDecimal.ONE.add(r).pow(-months, mc), mc);
            instalment = p.multiply(r, mc).divide(denominator, mc).setScale(0, RoundingMode.HALF_EVEN).longValueExact();
        }
        List<ScheduleRow> rows = new ArrayList<>();
        long remaining = principalMinor;
        for (int n = 1; n <= months; n++) {
            long interest = BigDecimal.valueOf(remaining).multiply(r, mc).setScale(0, RoundingMode.HALF_EVEN).longValueExact();
            long principalPart = n == months ? remaining : Math.min(remaining, instalment - interest);
            long due = principalPart + interest;
            rows.add(new ScheduleRow(n, firstDueDate.plusMonths(n - 1), remaining, interest, principalPart, due, remaining - principalPart));
            remaining -= principalPart;
        }
        return rows;
    }

    /** Total cost of credit = all interest over the loan; shown to the borrower before they accept. */
    public static long totalInterest(List<ScheduleRow> rows) {
        return rows.stream().mapToLong(ScheduleRow::interestMinor).sum();
    }
}
