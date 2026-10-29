package com.collins.platform.lending.collections;

import com.collins.platform.commons.clients.AccountsClient;
import com.collins.platform.commons.clients.PricingClient;
import com.collins.platform.commons.clients.Quote;
import com.collins.platform.commons.events.EventEnvelope;
import com.collins.platform.commons.events.EventReader;
import com.collins.platform.commons.events.ProcessedEvents;
import com.collins.platform.commons.events.Topics;
import com.collins.platform.commons.exception.BusinessRuleException;
import com.collins.platform.commons.exception.NotFoundException;
import com.collins.platform.commons.exception.PlatformException;
import com.collins.platform.commons.jdbc.Ids;
import com.collins.platform.commons.money.CurrencyUnits;
import com.collins.platform.lending.collections.CollectionsRepository.RecoveryCase;
import com.collins.platform.lending.collections.CollectionsRepository.Step;
import com.collins.platform.lending.loans.LoanModels.Loan;
import com.collins.platform.lending.loans.LoanService;
import com.collins.platform.ledger.client.LedgerBalance;
import com.collins.platform.ledger.client.LedgerClient;
import com.collins.platform.ledger.client.Posting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Collections module: recovers what a person or merchant owes from all of their accounts (set-off), in a fixed order:
 * same currency (savings first, then main), then other currencies at the mid rate, then auto-sweep of money that
 * arrives later, then write-off. Every step is its own balanced, idempotent ledger transaction.
 */
@Service
public class CollectionsService {

    private static final Logger log = LoggerFactory.getLogger(CollectionsService.class);

    public record LoanInstalmentMissed(String loanId, String entityId, String borrowerType, String borrowerId, String currency,
                                       long overdueMinor, int dpd) {
    }

    private final CollectionsRepository repo;
    private final AccountsClient accounts;
    private final PricingClient pricing;
    private final LedgerClient ledger;
    private final LoanService loans;
    private final ProcessedEvents processed;
    private final EventReader events;

    public CollectionsService(CollectionsRepository repo, AccountsClient accounts, PricingClient pricing, LedgerClient ledger,
                              LoanService loans, ProcessedEvents processed, EventReader events) {
        this.repo = repo;
        this.accounts = accounts;
        this.pricing = pricing;
        this.ledger = ledger;
        this.loans = loans;
        this.processed = processed;
        this.events = events;
    }

    // ------------------------------------------------------------------ opening cases

    /**
     * A wallet went negative (e.g. a top-up was charged back). Reclassify the debt so the wallet shows zero and the
     * debt is an asset: Dr 1310 Customer receivable / Cr 2010 Customer wallet. Then open a case.
     */
    public RecoveryCase fromNegativeWallet(String walletId) {
        AccountsClient.Wallet w = accounts.wallet(walletId);
        long available = w.availableMinor() == null ? 0 : w.availableMinor();
        if (available >= 0) {
            throw new BusinessRuleException("NOT_NEGATIVE", "Wallet " + walletId + " is not negative");
        }
        long owed = -available;
        String debtAccount = "1310:" + w.individualId();
        ledger.post(Posting.of(w.entityId(), "DEBT_RECLASSIFICATION", "reclass:" + walletId + ":" + owed)
                .description("Negative wallet becomes a receivable").reference("wallet", walletId)
                .debit(debtAccount, owed, w.currency())
                .credit(w.ledgerAccount(), owed, w.currency()));
        return open(w.entityId(), "INDIVIDUAL", w.individualId(), "BALANCE", debtAccount, w.currency(), owed,
                "Negative wallet " + walletId, walletId);
    }

    /** A merchant balance went negative (refunds or chargebacks). Dr 1300 Merchant receivable / Cr 2000 Merchant payable. */
    public RecoveryCase fromNegativeMerchant(String merchantId) {
        AccountsClient.Merchant m = accounts.merchant(merchantId);
        long available = ledger.balance(m.entityId(), m.payableAccount(), m.settlementCurrency()).availableMinor();
        if (available >= 0) {
            throw new BusinessRuleException("NOT_NEGATIVE", "Merchant " + merchantId + " balance is not negative");
        }
        long owed = -available;
        String debtAccount = "1300:" + merchantId;
        ledger.post(Posting.of(m.entityId(), "DEBT_RECLASSIFICATION", "reclass:" + merchantId + ":" + owed)
                .description("Negative merchant balance becomes a receivable").reference("merchant", merchantId)
                .debit(debtAccount, owed, m.settlementCurrency())
                .credit(m.payableAccount(), owed, m.settlementCurrency()));
        return open(m.entityId(), "MERCHANT", merchantId, "BALANCE", debtAccount, m.settlementCurrency(), owed,
                "Negative merchant balance", merchantId);
    }

    /** Overdue loan instalments: lending publishes LoanInstalmentMissed; collections opens a case and starts recovering. */
    @KafkaListener(topics = Topics.LOAN_INSTALMENT_MISSED, groupId = "lending-service-collections")
    public void onInstalmentMissed(String message) {
        EventEnvelope envelope = events.read(message);
        if (!processed.markProcessed("collections", envelope.meta().eventId())) {
            return;
        }
        LoanInstalmentMissed e = events.payload(envelope, LoanInstalmentMissed.class);
        RecoveryCase c = repo.openForReference(e.loanId()).orElseGet(() -> open(e.entityId(), e.borrowerType(), e.borrowerId(),
                "LOAN", "loan:" + e.loanId(), e.currency(), e.overdueMinor(), "Missed instalment, " + e.dpd() + " DPD", e.loanId()));
        try {
            run(c.id());
        } catch (PlatformException ex) {
            log.info("Recovery for case {} could not complete now: {}", c.id(), ex.getMessage());
        }
    }

    public RecoveryCase open(String entityId, String debtorType, String debtorId, String debtKind, String debtAccount, String currency,
                             long amountMinor, String reason, String referenceId) {
        RecoveryCase c = new RecoveryCase(Ids.newId("rc"), entityId, debtorType, debtorId, debtKind, debtAccount, currency,
                amountMinor, 0, 0, "OPEN", reason, referenceId);
        repo.insert(c);
        return c;
    }

    // ------------------------------------------------------------------ recovering

    public RecoveryCase run(String caseId) {
        RecoveryCase c = get(caseId);
        if (c.remainingMinor() <= 0 || "WRITTEN_OFF".equals(c.status())) {
            return c;
        }
        long recovered = 0;
        if ("INDIVIDUAL".equals(c.debtorType())) {
            recovered = recoverFromWallets(c);
        } else if ("BALANCE".equals(c.debtKind())) {
            recovered = recoverFromMerchant(c);
        }
        long total = c.recoveredMinor() + recovered;
        String status = total >= c.amountMinor() - c.writtenOffMinor() ? "RECOVERED" : total > 0 ? "PARTIALLY_RECOVERED" : "OPEN";
        repo.updateAmounts(caseId, total, c.writtenOffMinor(), status, c.amountMinor());
        return get(caseId);
    }

    private long recoverFromWallets(RecoveryCase c) {
        List<AccountsClient.Wallet> wallets = new ArrayList<>(accounts.wallets(c.debtorId()));
        // Recovery order: same currency first (savings before main), then other currencies.
        wallets.sort(Comparator.comparing((AccountsClient.Wallet w) -> !w.currency().equals(c.currency()))
                .thenComparing(w -> !"SAVINGS".equals(w.kind())));
        long remaining = c.remainingMinor();
        long recovered = 0;
        int step = repo.steps(c.id()).size();
        for (AccountsClient.Wallet w : wallets) {
            long available = w.availableMinor() == null ? 0 : w.availableMinor();
            if (remaining <= 0 || available <= 0) {
                continue;
            }
            step++;
            String key = "recovery:" + c.id() + ":" + step;
            if (w.currency().equals(c.currency())) {
                long take = Math.min(available, remaining);
                String txn;
                if ("LOAN".equals(c.debtKind())) {
                    txn = loans.repay(c.referenceId(), take, w.ledgerAccount(), key).ledgerTxnId();
                } else {
                    txn = ledger.post(Posting.of(c.entityId(), "RECOVERY", key).description("Recovery for case " + c.id())
                            .reference("recovery_case", c.id())
                            .debitNoOverdraft(w.ledgerAccount(), take, w.currency())
                            .credit(c.debtAccount(), take, c.currency())).transactionId();
                }
                repo.insertStep(new Step(Ids.newId("rs"), c.id(), "SAME_CURRENCY", w.ledgerAccount(), w.currency(), take, take, null, txn));
                remaining -= take;
                recovered += take;
            } else if ("BALANCE".equals(c.debtKind())) {
                long[] fx = crossCurrency(c, w, available, remaining, key);
                remaining -= fx[1];
                recovered += fx[1];
            }
        }
        return recovered;
    }

    /**
     * Cross-currency step at the mid-market rate (no spread on recoveries), joined by 1400 FX clearing:
     * Dr wallet (source ccy) / Cr 1400 (source ccy) / Dr 1400 (debt ccy) / Cr debt account (debt ccy) / Cr 4900 rounding.
     * @return {sourceTaken, debtCredited}
     */
    private long[] crossCurrency(RecoveryCase c, AccountsClient.Wallet w, long available, long remaining, String key) {
        Quote.Fx full = pricing.fx(available, w.currency(), c.currency());
        BigDecimal mid = full.midRate();
        long source;
        long debt;
        if (full.midConvertedMinor() <= remaining) {
            source = available;
            debt = full.midConvertedMinor();
        } else {
            BigDecimal neededMajor = CurrencyUnits.toMajor(remaining, c.currency()).divide(mid, MathContext.DECIMAL64)
                    .setScale(CurrencyUnits.digits(w.currency()), RoundingMode.UP);
            source = Math.min(available, CurrencyUnits.toMinor(neededMajor, w.currency()));
            debt = CurrencyUnits.convert(source, w.currency(), mid, c.currency());
        }
        long applied = Math.min(debt, remaining);
        String txn = ledger.post(Posting.of(c.entityId(), "RECOVERY_FX", key)
                .description("Cross-currency recovery for case " + c.id()).reference("recovery_case", c.id())
                .meta("mid_rate", mid.toPlainString())
                .debitNoOverdraft(w.ledgerAccount(), source, w.currency())
                .credit("1400", source, w.currency())
                .debit("1400", debt, c.currency())
                .credit(c.debtAccount(), applied, c.currency())
                .credit("4900", debt - applied, c.currency())).transactionId();
        repo.insertStep(new Step(Ids.newId("rs"), c.id(), "CROSS_CURRENCY", w.ledgerAccount(), w.currency(), source, applied, mid, txn));
        return new long[]{source, applied};
    }

    private long recoverFromMerchant(RecoveryCase c) {
        AccountsClient.Merchant m = accounts.merchant(c.debtorId());
        LedgerBalance payable = ledger.balance(c.entityId(), m.payableAccount(), c.currency());
        long take = Math.min(Math.max(0, payable.availableMinor()), c.remainingMinor());
        if (take <= 0) {
            return 0;
        }
        String key = "recovery:" + c.id() + ":" + (repo.steps(c.id()).size() + 1);
        String txn = ledger.post(Posting.of(c.entityId(), "RECOVERY", key).description("Recovery from merchant balance")
                .reference("recovery_case", c.id())
                .debitNoOverdraft(m.payableAccount(), take, c.currency())
                .credit(c.debtAccount(), take, c.currency())).transactionId();
        repo.insertStep(new Step(Ids.newId("rs"), c.id(), "MERCHANT_BALANCE", m.payableAccount(), c.currency(), take, take, null, txn));
        return take;
    }

    // ------------------------------------------------------------------ write-off and recovery after write-off

    /** Gives up on the rest. Balances: Dr 5300 Chargeback loss / Cr debt account. Loans: the loan's own write-off. */
    public RecoveryCase writeOff(String caseId) {
        RecoveryCase c = get(caseId);
        long remaining = c.remainingMinor();
        if (remaining <= 0) {
            return c;
        }
        if ("LOAN".equals(c.debtKind())) {
            Loan loan = loans.writeOff(c.referenceId(), "Collections write-off, case " + caseId);
            remaining = Math.max(remaining, 0);
            repo.insertStep(new Step(Ids.newId("rs"), caseId, "WRITE_OFF", loan.principalAccount(), c.currency(), 0, remaining, null, null));
        } else {
            String txn = ledger.post(Posting.of(c.entityId(), "DEBT_WRITE_OFF", "recovery:" + caseId + ":writeoff")
                    .description("Write-off of case " + caseId).reference("recovery_case", caseId)
                    .debit("5300", remaining, c.currency())
                    .credit(c.debtAccount(), remaining, c.currency())).transactionId();
            repo.insertStep(new Step(Ids.newId("rs"), caseId, "WRITE_OFF", "5300", c.currency(), remaining, remaining, null, txn));
        }
        repo.updateAmounts(caseId, c.recoveredMinor(), c.writtenOffMinor() + remaining, "WRITTEN_OFF", c.amountMinor());
        return get(caseId);
    }

    /**
     * Money arrives after a write-off: it reduces the loss instead of counting as new revenue.
     * Dr wallet / Cr 5300 (balances) or Cr 5100 (loans).
     */
    public RecoveryCase sweepAfterWriteOff(String caseId, String walletId, long amountMinor) {
        RecoveryCase c = get(caseId);
        if (!"WRITTEN_OFF".equals(c.status()) && !"RECOVERED_AFTER_WRITE_OFF".equals(c.status())) {
            throw new BusinessRuleException("NOT_WRITTEN_OFF", "Use /run for cases that are not written off");
        }
        AccountsClient.Wallet w = accounts.wallet(walletId);
        if (!w.currency().equals(c.currency())) {
            throw new BusinessRuleException("CURRENCY_MISMATCH", "Sweep from a wallet in " + c.currency());
        }
        long available = w.availableMinor() == null ? 0 : w.availableMinor();
        long take = Math.min(Math.min(available, c.collectableMinor()), amountMinor <= 0 ? Long.MAX_VALUE : amountMinor);
        if (take <= 0) {
            throw new BusinessRuleException("NOTHING_TO_SWEEP", "Nothing available to sweep");
        }
        String lossAccount = "LOAN".equals(c.debtKind()) ? "5100" : "5300";
        String key = "recovery:" + caseId + ":sweep:" + (repo.steps(caseId).size() + 1);
        String txn = ledger.post(Posting.of(c.entityId(), "RECOVERY_AFTER_WRITE_OFF", key)
                .description("Recovery after write-off, case " + caseId).reference("recovery_case", caseId)
                .debitNoOverdraft(w.ledgerAccount(), take, c.currency())
                .credit(lossAccount, take, c.currency())).transactionId();
        repo.insertStep(new Step(Ids.newId("rs"), caseId, "SWEEP_AFTER_WRITE_OFF", w.ledgerAccount(), c.currency(), take, take, null, txn));
        long recovered = c.recoveredMinor() + take;
        String status = recovered >= c.amountMinor() ? "RECOVERED_AFTER_WRITE_OFF" : "WRITTEN_OFF";
        repo.updateAmounts(caseId, recovered, Math.max(0, c.writtenOffMinor() - take), status, c.amountMinor());
        return get(caseId);
    }

    public RecoveryCase get(String caseId) {
        return repo.find(caseId, false).orElseThrow(() -> new NotFoundException("Recovery case", caseId));
    }

    public List<RecoveryCase> byDebtor(String debtorId) {
        return repo.byDebtor(debtorId);
    }

    public List<Step> steps(String caseId) {
        return repo.steps(caseId);
    }
}
