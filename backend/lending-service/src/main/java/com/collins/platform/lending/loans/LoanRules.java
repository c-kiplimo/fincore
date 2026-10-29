package com.collins.platform.lending.loans;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Lending rules as pure functions: repayment waterfall, IFRS 9 staging, fee recognition. */
public final class LoanRules {

    public record Split(long feesPart, long interestPart, long principalPart, long leftover) {
    }

    private LoanRules() {
    }

    /** Waterfall: a repayment pays fees first, then interest, then principal. */
    public static Split waterfall(long amount, long feesDue, long interestDue, long principalDue) {
        long fees = Math.min(amount, feesDue);
        long rest = amount - fees;
        long interest = Math.min(rest, interestDue);
        rest -= interest;
        long principal = Math.min(rest, principalDue);
        rest -= principal;
        return new Split(fees, interest, principal, rest);
    }

    /** IFRS 9 stage from days past due: 1 performing, 2 significant increase in risk (30+), 3 credit-impaired (90+). */
    public static int stage(int dpd) {
        if (dpd >= 90) {
            return 3;
        }
        return dpd >= 30 ? 2 : 1;
    }

    /** Illustrative expected-loss rates per stage (real lenders estimate these from data). */
    public static BigDecimal lossRate(int stage) {
        return switch (stage) {
            case 3 -> new BigDecimal("0.50");
            case 2 -> new BigDecimal("0.10");
            default -> new BigDecimal("0.01");
        };
    }

    public static long allowanceTarget(long carryingMinor, int stage) {
        return BigDecimal.valueOf(Math.max(0, carryingMinor)).multiply(lossRate(stage)).setScale(0, RoundingMode.HALF_EVEN).longValueExact();
    }

    /**
     * Merchant financing: the fixed fee is earned in proportion to what is repaid.
     * share = fee x repaid / (principal + fee); the final repayment takes whatever is left.
     */
    public static long feeEarned(long repaid, long principal, long fee, long unearnedRemaining, boolean finalRepayment) {
        if (finalRepayment) {
            return unearnedRemaining;
        }
        long share = BigDecimal.valueOf(fee).multiply(BigDecimal.valueOf(repaid))
                .divide(BigDecimal.valueOf(principal + fee), 0, RoundingMode.HALF_EVEN).longValueExact();
        return Math.min(share, unearnedRemaining);
    }
}
