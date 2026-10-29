package com.collins.platform.lending.loans;

import java.time.LocalDate;

public final class LoanModels {

    private LoanModels() {
    }

    public record Product(String code, String kind, String name, int annualRateBp, int termMonths, int fixedFeeBp,
                          int withholdBp, long lateFeeMinor, int graceDays, long maxPrincipalMinor) {
    }

    public record Loan(String id, String entityId, String jurisdiction, String productCode, String kind, String borrowerType,
                       String borrowerId, String borrowerAccount, String currency, long principalMinor, long feeMinor,
                       int annualRateBp, int termMonths, String status, int stage, int dpd, long outstandingMinor,
                       long interestDueMinor, long feesDueMinor, long unearnedFeeMinor, long allowanceMinor,
                       LocalDate disbursedOn, LocalDate lastAccrualDate) {

        public boolean instalment() {
            return "INSTALMENT_LOAN".equals(kind);
        }

        /** What the borrower still owes in total. */
        public long totalOwedMinor() {
            return outstandingMinor + interestDueMinor + feesDueMinor;
        }

        /** Net carrying amount: what the receivable is worth on the balance sheet. */
        public long carryingMinor() {
            return totalOwedMinor() - unearnedFeeMinor;
        }

        public String principalAccount() {
            return (instalment() ? "1230:" : "1200:") + id;
        }

        /** Returns a copy with the fields that change over a loan's life replaced. */
        public Loan update(String newStatus, int newStage, int newDpd, long outstanding, long interestDue, long feesDue,
                           long unearnedFee, long allowance, long fee, LocalDate disbursed, LocalDate lastAccrual) {
            return new Loan(id, entityId, jurisdiction, productCode, kind, borrowerType, borrowerId, borrowerAccount, currency,
                    principalMinor, fee, annualRateBp, termMonths, newStatus, newStage, newDpd, outstanding, interestDue, feesDue,
                    unearnedFee, allowance, disbursed, lastAccrual);
        }

        public Loan withBalances(long outstanding, long interestDue, long feesDue, long unearnedFee, long allowance) {
            return update(status, stage, dpd, outstanding, interestDue, feesDue, unearnedFee, allowance, feeMinor, disbursedOn,
                    lastAccrualDate);
        }

        public Loan withStatus(String newStatus) {
            return update(newStatus, stage, dpd, outstandingMinor, interestDueMinor, feesDueMinor, unearnedFeeMinor,
                    allowanceMinor, feeMinor, disbursedOn, lastAccrualDate);
        }
    }

    public record ScheduleRow(String loanId, int n, LocalDate dueDate, long principalDue, long interestDue, long paidMinor,
                              boolean lateFeeDone) {
        public long dueMinor() {
            return principalDue + interestDue;
        }

        public long unpaidMinor() {
            return Math.max(0, dueMinor() - paidMinor);
        }
    }

    public record Repayment(String id, String loanId, long amountMinor, long feesPart, long interestPart, long principalPart,
                            String sourceAccount, String ledgerTxnId) {
    }
}
