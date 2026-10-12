package com.collins.platform.lending.loans;

import com.collins.platform.commons.clients.AccountsClient;
import com.collins.platform.commons.clients.PricingClient;
import com.collins.platform.commons.events.EventEnvelope;
import com.collins.platform.commons.events.EventReader;
import com.collins.platform.commons.events.ProcessedEvents;
import com.collins.platform.commons.events.Topics;
import com.collins.platform.commons.exception.BusinessRuleException;
import com.collins.platform.commons.exception.NotFoundException;
import com.collins.platform.commons.jdbc.Ids;
import com.collins.platform.commons.money.CurrencyUnits;
import com.collins.platform.commons.outbox.OutboxPublisher;
import com.collins.platform.lending.loans.LoanModels.Loan;
import com.collins.platform.lending.loans.LoanModels.Product;
import com.collins.platform.lending.loans.LoanModels.Repayment;
import com.collins.platform.lending.loans.LoanModels.ScheduleRow;
import com.collins.platform.ledger.client.LedgerClient;
import com.collins.platform.ledger.client.Posting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

/**
 * Loans: merchant financing (repaid from card sales) and personal instalment loans (repaid monthly with interest).
 * Every change in what is owed is a ledger posting; the loan row keeps a mirror of the balances for decisions.
 */
@Service
public class LoanService {

    private static final Logger log = LoggerFactory.getLogger(LoanService.class);
    private static final Map<String, String> JURISDICTION = Map.of("KE", "KE", "UK", "GB", "US", "US");

    public record LoanInstalmentMissed(String loanId, String entityId, String borrowerType, String borrowerId, String currency,
                                       long overdueMinor, int dpd) {
    }

    public record PaymentCaptured(String paymentId, String entityId, String merchantId, long amountMinor, String currency,
                                  long netMinor) {
    }

    public record PayoffQuote(String loanId, String currency, long principalMinor, long interestMinor, long feesMinor, long totalMinor) {
    }

    private final LoanRepository repo;
    private final AccountsClient accounts;
    private final PricingClient pricing;
    private final LedgerClient ledger;
    private final OutboxPublisher outbox;
    private final ProcessedEvents processed;
    private final EventReader events;

    public LoanService(LoanRepository repo, AccountsClient accounts, PricingClient pricing, LedgerClient ledger,
                       OutboxPublisher outbox, ProcessedEvents processed, EventReader events) {
        this.repo = repo;
        this.accounts = accounts;
        this.pricing = pricing;
        this.ledger = ledger;
        this.outbox = outbox;
        this.processed = processed;
        this.events = events;
    }

    // ------------------------------------------------------------------ origination

    /** Application + eligibility (Specification pattern, kept simple): limits, no bad loans, active borrower. */
    public Loan apply(String productCode, String borrowerType, String borrowerId, String walletId, long principalMinor) {
        Product product = product(productCode);
        if (principalMinor <= 0 || principalMinor > product.maxPrincipalMinor()) {
            throw new BusinessRuleException("INELIGIBLE_AMOUNT", "Principal must be between 1 and " + product.maxPrincipalMinor());
        }
        if (repo.hasBadLoan(borrowerId)) {
            throw new BusinessRuleException("INELIGIBLE_HISTORY", "Borrower has a defaulted or written-off loan");
        }
        String entityId;
        String currency;
        String account;
        if ("MERCHANT".equals(borrowerType)) {
            if (!"MERCHANT_FINANCING".equals(product.kind())) {
                throw new BusinessRuleException("WRONG_PRODUCT", "Merchants can only take merchant financing");
            }
            AccountsClient.Merchant m = accounts.merchant(borrowerId);
            if (!"ACTIVE".equals(m.status())) {
                throw new BusinessRuleException("INELIGIBLE_MERCHANT", "Merchant is not active");
            }
            if (repo.activeFinancingFor(borrowerId).isPresent()) {
                throw new BusinessRuleException("ALREADY_FINANCED", "Merchant already has active financing");
            }
            entityId = m.entityId();
            currency = m.settlementCurrency();
            account = m.payableAccount();
        } else if ("INDIVIDUAL".equals(borrowerType)) {
            if (!"INSTALMENT_LOAN".equals(product.kind())) {
                throw new BusinessRuleException("WRONG_PRODUCT", "Individuals can only take instalment loans");
            }
            if (walletId == null) {
                throw new BusinessRuleException("WALLET_REQUIRED", "walletId is required to disburse a personal loan");
            }
            AccountsClient.Wallet w = accounts.wallet(walletId);
            if (!borrowerId.equals(w.individualId())) {
                throw new BusinessRuleException("WALLET_NOT_OWNED", "The wallet does not belong to the borrower");
            }
            entityId = w.entityId();
            currency = w.currency();
            account = w.ledgerAccount();
        } else {
            throw new BusinessRuleException("INVALID_BORROWER", "borrowerType must be MERCHANT or INDIVIDUAL");
        }
        Loan loan = new Loan(Ids.newId("ln"), entityId, JURISDICTION.getOrDefault(entityId, entityId), product.code(), product.kind(),
                borrowerType, borrowerId, account, currency, principalMinor, 0, product.annualRateBp(), product.termMonths(),
                "APPROVED", 1, 0, 0, 0, 0, 0, 0, null, null);
        repo.insert(loan);
        return get(loan.id());
    }

    /**
     * Instalment loan: Dr 1230 Loan receivable / Cr 2010 Customer wallet (no cash leaves until the borrower withdraws).
     * Merchant financing: Dr 1200 Advance receivable (principal + fee) / Cr 2000 Merchant payable (principal)
     * / Cr 1210 Unearned financing fee (fee).
     */
    public Loan disburse(String loanId) {
        Loan loan = get(loanId);
        if (!"APPROVED".equals(loan.status())) {
            return loan;
        }
        Product product = product(loan.productCode());
        LocalDate today = LocalDate.now();
        if (loan.instalment()) {
            PricingClient.Schedule schedule = pricing.schedule(loan.principalMinor(), loan.currency(), loan.annualRateBp(),
                    loan.termMonths(), today.plusMonths(1), loan.jurisdiction());
            ledger.post(Posting.of(loan.entityId(), "LOAN_DISBURSEMENT", "loan:" + loan.id() + ":disburse")
                    .description("Disbursement of " + loan.id()).reference("loan", loan.id())
                    .debit(loan.principalAccount(), loan.principalMinor(), loan.currency())
                    .credit(loan.borrowerAccount(), loan.principalMinor(), loan.currency()));
            schedule.rows().forEach(r -> repo.insertScheduleRow(new ScheduleRow(loan.id(), r.number(), r.dueDate(),
                    r.principalMinor(), r.interestMinor(), 0, false)));
            repo.save(loan.update("ACTIVE", 1, 0, loan.principalMinor(), 0, 0, 0, 0, 0, today, today));
        } else {
            long fee = CurrencyUnits.applyRate(loan.principalMinor(), BigDecimal.valueOf(product.fixedFeeBp()).divide(BigDecimal.valueOf(10_000)));
            ledger.post(Posting.of(loan.entityId(), "FINANCING_DISBURSEMENT", "loan:" + loan.id() + ":disburse")
                    .description("Merchant financing " + loan.id()).reference("loan", loan.id())
                    .debit(loan.principalAccount(), loan.principalMinor() + fee, loan.currency())
                    .credit(loan.borrowerAccount(), loan.principalMinor(), loan.currency())
                    .credit("1210:" + loan.id(), fee, loan.currency()));
            repo.save(loan.update("ACTIVE", 1, 0, loan.principalMinor() + fee, 0, 0, fee, 0, fee, today, today));
        }
        outbox.publish(Topics.LOAN_DISBURSED, loan.borrowerId(), "LoanDisbursed", get(loanId));
        return provision(get(loanId), today);
    }

    // ------------------------------------------------------------------ interest and repayment

    /** Daily accrual: Dr 1240 Interest receivable / Cr 4110 Interest income. Stops at stage 3 (non-accrual). */
    public Loan accrue(String loanId, LocalDate asOf) {
        Loan loan = get(loanId);
        if (!loan.instalment() || loan.lastAccrualDate() == null || !loan.lastAccrualDate().isBefore(asOf)
                || !List.of("ACTIVE", "DELINQUENT").contains(loan.status()) || loan.stage() == 3 || loan.outstandingMinor() == 0) {
            return loan;
        }
        long interest = pricing.accrue(loan.outstandingMinor(), loan.annualRateBp(), loan.lastAccrualDate(), asOf, loan.jurisdiction())
                .interestMinor();
        if (interest > 0) {
            ledger.post(Posting.of(loan.entityId(), "INTEREST_ACCRUAL", "accrual:" + loan.id() + ":" + asOf)
                    .description("Interest " + loan.lastAccrualDate() + " to " + asOf).reference("loan", loan.id())
                    .effectiveDate(asOf.toString())
                    .debit("1240:" + loan.id(), interest, loan.currency())
                    .credit("4110", interest, loan.currency()));
        }
        repo.save(loan.update(loan.status(), loan.stage(), loan.dpd(), loan.outstandingMinor(), loan.interestDueMinor() + interest,
                loan.feesDueMinor(), loan.unearnedFeeMinor(), loan.allowanceMinor(), loan.feeMinor(), loan.disbursedOn(), asOf));
        return get(loanId);
    }

    /**
     * Repayment. Instalment loans use the waterfall (fees, then interest, then principal):
     * Dr source / Cr 1250 / Cr 1240 / Cr 1230. Merchant financing: Dr source / Cr 1200, and the matching share
     * of the fee is earned: Dr 1210 / Cr 4100.
     */
    public Repayment repay(String loanId, long amountMinor, String sourceAccount, String key) {
        Loan loan = accrue(loanId, LocalDate.now());
        if (!List.of("ACTIVE", "DELINQUENT", "DEFAULTED").contains(loan.status())) {
            throw new BusinessRuleException("LOAN_NOT_REPAYABLE", "Loan " + loanId + " is " + loan.status());
        }
        String source = sourceAccount == null ? loan.borrowerAccount() : sourceAccount;
        String repaymentId = Ids.newId("rp");
        String idem = key == null ? "repayment:" + repaymentId : key;
        Repayment repayment;
        if (loan.instalment()) {
            LoanRules.Split split = LoanRules.waterfall(amountMinor, loan.feesDueMinor(), loan.interestDueMinor(), loan.outstandingMinor());
            long used = amountMinor - split.leftover();
            if (used <= 0) {
                throw new BusinessRuleException("NOTHING_DUE", "Nothing is owed on this loan");
            }
            String txn = ledger.post(Posting.of(loan.entityId(), "LOAN_REPAYMENT", idem)
                    .description("Repayment of " + loan.id()).reference("loan", loan.id())
                    .debitNoOverdraft(source, used, loan.currency())
                    .credit("1250:" + loan.id(), split.feesPart(), loan.currency())
                    .credit("1240:" + loan.id(), split.interestPart(), loan.currency())
                    .credit("1230:" + loan.id(), split.principalPart(), loan.currency())).transactionId();
            applyToSchedule(loan.id(), split.interestPart() + split.principalPart());
            Loan updated = loan.withBalances(loan.outstandingMinor() - split.principalPart(), loan.interestDueMinor() - split.interestPart(),
                    loan.feesDueMinor() - split.feesPart(), 0, loan.allowanceMinor());
            repo.save(updated);
            repayment = new Repayment(repaymentId, loanId, used, split.feesPart(), split.interestPart(), split.principalPart(), source, txn);
        } else {
            long pay = Math.min(amountMinor, loan.outstandingMinor());
            if (pay <= 0) {
                throw new BusinessRuleException("NOTHING_DUE", "Nothing is owed on this loan");
            }
            boolean last = pay == loan.outstandingMinor();
            long earned = LoanRules.feeEarned(pay, loan.principalMinor(), loan.feeMinor(), loan.unearnedFeeMinor(), last);
            String txn = ledger.post(Posting.of(loan.entityId(), "FINANCING_REPAYMENT", idem)
                    .description("Financing repayment " + loan.id()).reference("loan", loan.id())
                    .debitNoOverdraft(source, pay, loan.currency())
                    .credit("1200:" + loan.id(), pay, loan.currency())
                    .debit("1210:" + loan.id(), earned, loan.currency())
                    .credit("4100", earned, loan.currency())).transactionId();
            repo.save(loan.withBalances(loan.outstandingMinor() - pay, 0, 0, loan.unearnedFeeMinor() - earned, loan.allowanceMinor()));
            repayment = new Repayment(repaymentId, loanId, pay, 0, earned, pay, source, txn);
        }
        repo.insertRepayment(repayment);
        closeIfRepaid(loanId);
        return repayment;
    }

    public PayoffQuote payoffQuote(String loanId) {
        Loan loan = accrue(loanId, LocalDate.now());
        return new PayoffQuote(loanId, loan.currency(), loan.outstandingMinor(), loan.interestDueMinor(), loan.feesDueMinor(),
                loan.totalOwedMinor());
    }

    /** Early repayment: accrue interest to today, then repay everything; no future interest is charged. */
    public Repayment repayInFull(String loanId, String sourceAccount) {
        PayoffQuote q = payoffQuote(loanId);
        return repay(loanId, q.totalMinor(), sourceAccount, "payoff:" + loanId);
    }

    private void applyToSchedule(String loanId, long amount) {
        long rest = amount;
        for (ScheduleRow row : repo.schedule(loanId)) {
            if (rest <= 0) {
                break;
            }
            long pay = Math.min(rest, row.unpaidMinor());
            if (pay > 0) {
                repo.addPaid(loanId, row.n(), pay);
                rest -= pay;
            }
        }
    }

    private void closeIfRepaid(String loanId) {
        Loan loan = get(loanId);
        if (loan.totalOwedMinor() == 0 && !"CLOSED".equals(loan.status())) {
            if (loan.allowanceMinor() > 0) { // nothing is at risk any more: release the allowance
                ledger.post(Posting.of(loan.entityId(), "ALLOWANCE_RELEASE", "allowance:" + loan.id() + ":release")
                        .description("Release allowance on repaid loan").reference("loan", loan.id())
                        .debit("1220:" + loan.id(), loan.allowanceMinor(), loan.currency())
                        .credit("5100", loan.allowanceMinor(), loan.currency()));
            }
            repo.save(loan.update("CLOSED", 1, 0, 0, 0, 0, 0, 0, loan.feeMinor(), loan.disbursedOn(), loan.lastAccrualDate()));
        }
    }

    // ------------------------------------------------------------------ merchant financing withholding

    /** Withholds a share of each card payment for merchants with active financing. */
    @KafkaListener(topics = Topics.PAYMENT_CAPTURED, groupId = "lending-service-withholding")
    public void onPaymentCaptured(String message) {
        EventEnvelope envelope = events.read(message);
        PaymentCaptured event = events.payload(envelope, PaymentCaptured.class);
        repo.activeFinancingFor(event.merchantId()).ifPresent(loan -> {
            if (!processed.markProcessed("withholding", envelope.meta().eventId())) {
                return;
            }
            Product product = product(loan.productCode());
            long withhold = Math.min(loan.outstandingMinor(), CurrencyUnits.applyRate(event.netMinor(),
                    BigDecimal.valueOf(product.withholdBp()).divide(BigDecimal.valueOf(10_000))));
            if (withhold > 0) {
                repay(loan.id(), withhold, "2000:" + event.merchantId(), "withhold:" + event.paymentId());
                log.info("Withheld {} {} from payment {} for financing {}", withhold, event.currency(), event.paymentId(), loan.id());
            }
        });
    }

    // ------------------------------------------------------------------ delinquency, provisioning, write-off

    /** Daily run for one loan: accrue, auto-collect due instalments, days past due, late fee, stage and allowance. */
    public Loan daily(String loanId, LocalDate asOf, int graceDays, long lateFeeMinor) {
        Loan loan = accrue(loanId, asOf);
        if (!loan.instalment() || !List.of("ACTIVE", "DELINQUENT", "DEFAULTED").contains(loan.status())) {
            return provision(loan, asOf);
        }
        long due = repo.schedule(loanId).stream().filter(r -> !r.dueDate().isAfter(asOf)).mapToLong(ScheduleRow::unpaidMinor).sum();
        if (due > 0) {
            long available = ledger.balance(loan.entityId(), loan.borrowerAccount(), loan.currency()).availableMinor();
            long pay = Math.min(available, due + loan.feesDueMinor());
            if (pay > 0) {
                repay(loanId, pay, loan.borrowerAccount(), "autodebit:" + loanId + ":" + asOf + ":" + pay);
            }
        }
        loan = get(loanId);
        ScheduleRow oldest = repo.schedule(loanId).stream().filter(r -> r.dueDate().isBefore(asOf) && r.unpaidMinor() > 0)
                .findFirst().orElse(null);
        int dpd = oldest == null ? 0 : (int) ChronoUnit.DAYS.between(oldest.dueDate(), asOf);
        long fees = loan.feesDueMinor();
        if (oldest != null && dpd > graceDays && !oldest.lateFeeDone() && lateFeeMinor > 0) {
            ledger.post(Posting.of(loan.entityId(), "LATE_FEE", "latefee:" + loanId + ":" + oldest.n())
                    .description("Late fee, instalment " + oldest.n()).reference("loan", loanId).effectiveDate(asOf.toString())
                    .debit("1250:" + loanId, lateFeeMinor, loan.currency())
                    .credit("4120", lateFeeMinor, loan.currency()));
            repo.markLateFee(loanId, oldest.n());
            fees += lateFeeMinor;
            long overdue = repo.schedule(loanId).stream().filter(r -> r.dueDate().isBefore(asOf)).mapToLong(ScheduleRow::unpaidMinor).sum();
            outbox.publish(Topics.LOAN_INSTALMENT_MISSED, loanId, "LoanInstalmentMissed", new LoanInstalmentMissed(loanId,
                    loan.entityId(), loan.borrowerType(), loan.borrowerId(), loan.currency(), overdue + fees, dpd));
        }
        String status = dpd >= 90 ? "DEFAULTED" : dpd > 0 ? "DELINQUENT" : "ACTIVE";
        repo.save(loan.update(status, LoanRules.stage(dpd), dpd, loan.outstandingMinor(), loan.interestDueMinor(), fees,
                loan.unearnedFeeMinor(), loan.allowanceMinor(), loan.feeMinor(), loan.disbursedOn(), loan.lastAccrualDate()));
        return provision(get(loanId), asOf);
    }

    /** IFRS 9 style provisioning: move the allowance to the stage's target. Dr 5100 / Cr 1220 (or the reverse). */
    public Loan provision(Loan loan, LocalDate asOf) {
        if (!List.of("ACTIVE", "DELINQUENT", "DEFAULTED").contains(loan.status())) {
            return loan;
        }
        long target = LoanRules.allowanceTarget(loan.carryingMinor(), loan.stage());
        long delta = target - loan.allowanceMinor();
        if (delta == 0) {
            return loan;
        }
        Posting posting = Posting.of(loan.entityId(), "LOSS_ALLOWANCE", "allowance:" + loan.id() + ":" + asOf + ":" + target)
                .description("Stage " + loan.stage() + " allowance").reference("loan", loan.id());
        if (delta > 0) {
            posting.debit("5100", delta, loan.currency()).credit("1220:" + loan.id(), delta, loan.currency());
        } else {
            posting.debit("1220:" + loan.id(), -delta, loan.currency()).credit("5100", -delta, loan.currency());
        }
        ledger.post(posting);
        repo.save(loan.withBalances(loan.outstandingMinor(), loan.interestDueMinor(), loan.feesDueMinor(), loan.unearnedFeeMinor(), target));
        return get(loan.id());
    }

    /**
     * Write-off: the allowance absorbs what it covers, the rest is credit loss expense, and every receivable goes to zero.
     * Merchant financing also cancels the unearned fee (it was never income).
     */
    public Loan writeOff(String loanId, String reason) {
        Loan loan = get(loanId);
        if ("WRITTEN_OFF".equals(loan.status()) || "CLOSED".equals(loan.status())) {
            return loan;
        }
        long allowance = loan.allowanceMinor();
        Posting posting = Posting.of(loan.entityId(), "LOAN_WRITE_OFF", "writeoff:" + loanId)
                .description("Write-off: " + (reason == null ? "" : reason)).reference("loan", loanId)
                .debit("1220:" + loanId, allowance, loan.currency())
                .debit("1210:" + loanId, loan.unearnedFeeMinor(), loan.currency());
        long loss = loan.carryingMinor() - allowance;
        if (loss > 0) {
            posting.debit("5100", loss, loan.currency());
        } else if (loss < 0) {
            posting.credit("5100", -loss, loan.currency());
        }
        posting.credit(loan.principalAccount(), loan.outstandingMinor(), loan.currency())
                .credit("1240:" + loanId, loan.interestDueMinor(), loan.currency())
                .credit("1250:" + loanId, loan.feesDueMinor(), loan.currency());
        ledger.post(posting);
        repo.save(loan.update("WRITTEN_OFF", 3, loan.dpd(), 0, 0, 0, 0, 0, loan.feeMinor(), loan.disbursedOn(), loan.lastAccrualDate()));
        return get(loanId);
    }

    /**
     * Restructuring: future instalments that have not been paid are replaced by a new schedule that spreads the
     * remaining principal over the remaining months plus the extension. Changing a schedule posts nothing in the ledger.
     */
    public Loan restructure(String loanId, int extraMonths) {
        Loan loan = get(loanId);
        if (!loan.instalment() || extraMonths <= 0 || loan.outstandingMinor() == 0) {
            throw new BusinessRuleException("CANNOT_RESTRUCTURE", "Only instalment loans with principal left can be extended");
        }
        LocalDate today = LocalDate.now();
        int removed = repo.deleteUnpaidFrom(loanId, today);
        int lastN = repo.schedule(loanId).stream().mapToInt(ScheduleRow::n).max().orElse(0);
        PricingClient.Schedule schedule = pricing.schedule(loan.outstandingMinor(), loan.currency(), loan.annualRateBp(),
                removed + extraMonths, today.plusMonths(1), loan.jurisdiction());
        schedule.rows().forEach(r -> repo.insertScheduleRow(new ScheduleRow(loanId, lastN + r.number(), r.dueDate(),
                r.principalMinor(), r.interestMinor(), 0, false)));
        log.info("Loan {} restructured over {} months", loanId, removed + extraMonths);
        return get(loanId);
    }

    // ------------------------------------------------------------------ queries

    public Loan get(String loanId) {
        return repo.find(loanId, false).orElseThrow(() -> new NotFoundException("Loan", loanId));
    }

    public Product product(String code) {
        return repo.product(code).orElseThrow(() -> new NotFoundException("Loan product", code));
    }

    public List<Product> products() {
        return repo.products();
    }

    public List<Loan> byBorrower(String borrowerId) {
        return repo.byBorrower(borrowerId);
    }

    public List<ScheduleRow> schedule(String loanId) {
        return repo.schedule(loanId);
    }

    public List<Repayment> repayments(String loanId) {
        return repo.repayments(loanId);
    }

    public List<Loan> active(String kind) {
        return repo.withStatus(List.of("ACTIVE", "DELINQUENT", "DEFAULTED"), kind);
    }
}
