package com.collins.platform.payouts;

import com.collins.platform.commons.clients.AccountsClient;
import com.collins.platform.commons.clients.PricingClient;
import com.collins.platform.commons.clients.Quote;
import com.collins.platform.commons.events.Topics;
import com.collins.platform.commons.exception.BusinessRuleException;
import com.collins.platform.commons.exception.NotFoundException;
import com.collins.platform.commons.exception.PlatformException;
import com.collins.platform.commons.jdbc.Ids;
import com.collins.platform.commons.outbox.OutboxPublisher;
import com.collins.platform.ledger.client.LedgerClient;
import com.collins.platform.ledger.client.Posting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.List;

/**
 * Payout saga (orchestration). Every step has a compensating step:
 * <ol>
 *   <li>Hold: pending ledger transaction Dr source (amount + fee + tax) / Cr 2300 Payout clearing / Cr 4300 fee / Cr tax.</li>
 *   <li>Send to the bank.</li>
 *   <li>PAID: commit the hold, then Dr 2300 Payout clearing / Cr 1000 Cash.
 *       FAILED: void the hold (compensation), so the money is back in the source balance.
 *       TIMEOUT / unreachable: leave it SENT and let the retry job ask the bank later.</li>
 * </ol>
 */
@Service
public class PayoutSaga {

    private static final Logger log = LoggerFactory.getLogger(PayoutSaga.class);
    private static final int MAX_ATTEMPTS = 10;

    public record PayoutEvent(String payoutId, String entityId, String beneficiaryType, String beneficiaryId,
                              long amountMinor, String currency, String status, String reason) {
    }

    private final PayoutRepository repo;
    private final AccountsClient accounts;
    private final PricingClient pricing;
    private final LedgerClient ledger;
    private final BankClient bank;
    private final OutboxPublisher outbox;
    private final TransactionTemplate tx;

    public PayoutSaga(PayoutRepository repo, AccountsClient accounts, PricingClient pricing, LedgerClient ledger,
                      BankClient bank, OutboxPublisher outbox, TransactionTemplate tx) {
        this.repo = repo;
        this.accounts = accounts;
        this.pricing = pricing;
        this.ledger = ledger;
        this.bank = bank;
        this.outbox = outbox;
        this.tx = tx;
    }

    public Payout start(String beneficiaryType, String beneficiaryId, long amountMinor, String destinationType, String destinationAccount) {
        String entityId;
        String currency;
        String source;
        String product;
        if ("MERCHANT".equals(beneficiaryType)) {
            AccountsClient.Merchant m = accounts.merchant(beneficiaryId);
            entityId = m.entityId();
            currency = m.settlementCurrency();
            source = m.payableAccount();
            product = "MERCHANT_PAYOUT";
        } else if ("WALLET".equals(beneficiaryType)) {
            AccountsClient.Wallet w = accounts.wallet(beneficiaryId);
            entityId = w.entityId();
            currency = w.currency();
            source = w.ledgerAccount();
            product = "WITHDRAWAL";
        } else {
            throw new BusinessRuleException("INVALID_BENEFICIARY", "beneficiaryType must be MERCHANT or WALLET");
        }

        String id = Ids.newId("po");
        Quote quote = pricing.quoteAndConsume(entityId, product, amountMinor, currency, "payout:" + id);
        Payout payout = new Payout(id, entityId, beneficiaryType, beneficiaryId, source, amountMinor, currency,
                quote.totalFeesMinor(), quote.totalTaxesMinor(), quote.totalDebitedMinor(), destinationType, destinationAccount,
                "CREATED", null, null, null, null, 0, Instant.now());
        repo.insert(payout);

        // Step 1: hold the money (pending). Fails here if the balance is too low; nothing to compensate.
        Posting hold = Posting.of(entityId, "PAYOUT_HOLD", "payout:" + id + ":hold").pending()
                .description(product + " " + id).reference("payout", id)
                .debitNoOverdraft(source, quote.totalDebitedMinor(), currency)
                .credit("2300", amountMinor, currency)
                .credit("4300", quote.totalFeesMinor(), currency);
        quote.taxes().forEach(t -> hold.credit(t.account(), t.amountMinor(), currency));
        String pendingTxn;
        try {
            pendingTxn = ledger.post(hold).transactionId();
        } catch (PlatformException ex) {
            repo.update(id, "FAILED", null, null, null, ex.getMessage(), false);
            throw ex;
        }
        repo.update(id, "SENDING", pendingTxn, null, null, null, false);
        return send(get(id));
    }

    /** Step 2 and 3: send to the bank and finish, compensate, or wait. Safe to call repeatedly. */
    Payout send(Payout p) {
        BankClient.BankResult result;
        try {
            result = "SENT".equals(p.status()) ? bank.status(p.id()) : bank.send(p);
        } catch (PlatformException ex) {
            log.warn("Bank unreachable for payout {}; will retry: {}", p.id(), ex.getMessage());
            repo.update(p.id(), p.status(), null, null, null, null, true);
            return get(p.id());
        }
        switch (result.status()) {
            case "PAID" -> complete(p, result.bankRef());
            case "FAILED" -> compensate(p, result.reason());
            default -> repo.update(p.id(), "SENT", null, null, null, null, true);
        }
        return get(p.id());
    }

    private void complete(Payout p, String bankRef) {
        ledger.commitPending(p.pendingTxnId());
        String confirm = ledger.post(Posting.of(p.entityId(), "PAYOUT_CONFIRMED", "payout:" + p.id() + ":confirm")
                .description("Bank confirmed payout " + p.id()).reference("payout", p.id())
                .debit("2300", p.amountMinor(), p.currency())
                .credit("1000", p.amountMinor(), p.currency())).transactionId();
        tx.executeWithoutResult(s -> {
            repo.update(p.id(), "PAID", null, confirm, bankRef, null, false);
            outbox.publish(Topics.PAYOUT_COMPLETED, p.beneficiaryId(), "PayoutCompleted", new PayoutEvent(p.id(), p.entityId(),
                    p.beneficiaryType(), p.beneficiaryId(), p.amountMinor(), p.currency(), "PAID", null));
        });
    }

    private void compensate(Payout p, String reason) {
        ledger.voidPending(p.pendingTxnId());
        tx.executeWithoutResult(s -> {
            repo.update(p.id(), "FAILED", null, null, null, reason, false);
            outbox.publish(Topics.PAYOUT_FAILED, p.beneficiaryId(), "PayoutFailed", new PayoutEvent(p.id(), p.entityId(),
                    p.beneficiaryType(), p.beneficiaryId(), p.amountMinor(), p.currency(), "FAILED", reason));
        });
    }

    public Payout retry(String payoutId) {
        Payout p = get(payoutId);
        if (!"SENDING".equals(p.status()) && !"SENT".equals(p.status())) {
            return p;
        }
        return send(p);
    }

    @Scheduled(fixedDelayString = "${payouts.retry-interval-ms:30000}")
    public void retryStuck() {
        List<Payout> stuck = repo.awaitingBank(MAX_ATTEMPTS);
        for (Payout p : stuck) {
            send(p);
        }
    }

    public Payout get(String id) {
        return repo.find(id).orElseThrow(() -> new NotFoundException("Payout", id));
    }

    public List<Payout> byBeneficiary(String beneficiaryId) {
        return repo.byBeneficiary(beneficiaryId);
    }
}
