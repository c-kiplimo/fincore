package com.collins.platform.lending.loans;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LoanRulesTest {

    @Test
    void waterfallPaysFeesThenInterestThenPrincipal() {
        LoanRules.Split s = LoanRules.waterfall(113_472, 0, 24_000, 1_200_000);
        assertThat(s.feesPart()).isZero();
        assertThat(s.interestPart()).isEqualTo(24_000);
        assertThat(s.principalPart()).isEqualTo(89_472);
    }

    @Test
    void lateFeeIsPaidFirst() {
        LoanRules.Split s = LoanRules.waterfall(10_000, 20_000, 5_000, 100_000);
        assertThat(s.feesPart()).isEqualTo(10_000);
        assertThat(s.interestPart()).isZero();
    }

    @Test
    void stagesFollowDaysPastDue() {
        assertThat(LoanRules.stage(0)).isEqualTo(1);
        assertThat(LoanRules.stage(45)).isEqualTo(2);
        assertThat(LoanRules.stage(120)).isEqualTo(3);
    }

    @Test
    void allowanceMovesFromStageOneToTwo() {
        // 9,000 owed: 1% = 90, 10% = 900 -> post 810 more credit loss expense
        assertThat(LoanRules.allowanceTarget(900_000, 2) - LoanRules.allowanceTarget(900_000, 1)).isEqualTo(81_000);
    }

    @Test
    void financingFeeEarnedInProportion() {
        // 10,000 principal + 1,000 fee; 100 repaid -> 9.09 of the fee earned
        assertThat(LoanRules.feeEarned(10_000, 1_000_000, 100_000, 100_000, false)).isEqualTo(909);
    }
}
