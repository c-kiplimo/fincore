package com.collins.platform.pricing.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PricingRulesTest {

    @Test
    void percentageFeeOnACardPayment() {
        assertThat(PricingRules.fee(100_000, 290, 0, 0, null)).isEqualTo(2_900); // 2.9% of 1,000.00
    }

    @Test
    void exciseOnTheFeeOnly() {
        assertThat(PricingRules.tax(1_000, new BigDecimal("0.15"))).isEqualTo(150); // 15% of a 10.00 fee
    }

    @Test
    void customerRateIncludesSpread() {
        assertThat(PricingRules.customerRate(new BigDecimal("130"), 100)).isEqualByComparingTo("128.70");
    }

    @Test
    void dayCountChangesInterest() {
        LocalDate from = LocalDate.of(2026, 1, 1);
        LocalDate to = from.plusDays(30);
        assertThat(InterestMath.accrue(1_000_000, 1200, from, to, DayCountConvention.ACT_365)).isEqualTo(9_863);
        assertThat(InterestMath.accrue(1_000_000, 1200, from, to, DayCountConvention.ACT_360)).isEqualTo(10_000);
    }

    @Test
    void annuityScheduleRepaysExactly() {
        // 12,000 KES, 24% a year (2% a month), 12 months -> instalment 1,134.72 (exact 1,134.7151...)
        List<InterestMath.ScheduleRow> rows = InterestMath.annuity(1_200_000, 2400, 12, LocalDate.of(2026, 2, 1));
        assertThat(rows.get(0).interestMinor()).isEqualTo(24_000);
        assertThat(rows.get(0).instalmentMinor()).isEqualTo(113_472);
        assertThat(rows.get(11).closingPrincipalMinor()).isZero();
        assertThat(rows.stream().mapToLong(InterestMath.ScheduleRow::principalMinor).sum()).isEqualTo(1_200_000);
    }
}
