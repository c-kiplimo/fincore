package com.collins.platform.payments.application;

import com.collins.platform.commons.clients.AccountsClient;
import com.collins.platform.commons.clients.PricingClient;
import com.collins.platform.commons.clients.Quote;
import com.collins.platform.commons.events.Topics;
import com.collins.platform.commons.exception.BusinessRuleException;
import com.collins.platform.commons.exception.NotFoundException;
import com.collins.platform.commons.jdbc.Ids;
import com.collins.platform.commons.outbox.OutboxPublisher;
import com.collins.platform.ledger.client.LedgerClient;
import com.collins.platform.ledger.client.Posting;
import com.collins.platform.payments.api.dto.CreatePaymentRequest;
import com.collins.platform.payments.api.dto.DisputeRequest;
import com.collins.platform.payments.api.dto.RefundRequest;
import com.collins.platform.payments.api.dto.SettlementRequest;
import com.collins.platform.payments.domain.PaymentIntent;
import com.collins.platform.payments.domain.PaymentStatus;
import com.collins.platform.payments.infrastructure.PaymentRepository;
import com.collins.platform.payments.infrastructure.PaymentRepository.Dispute;
import com.collins.platform.payments.infrastructure.PaymentRepository.Refund;
import com.collins.platform.payments.infrastructure.SimulatorClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Payment lifecycle. External calls (pricing, network, ledger) are idempotent and happen before the
 * local state change, which is then written together with its outbox event in one DB transaction.
 */
@Service
public class PaymentService {

    public record PaymentCaptured(String paymentId, String entityId, String merchantId, long amountMinor, String currency,
                                  long feeMinor, long taxMinor, long netMinor, Instant capturedAt) {
    }

    public record PaymentRefunded(String paymentId, String entityId, String merchantId, long amountMinor, String currency) {
    }

    public record DisputeOpened(String disputeId, String paymentId, String entityId, String merchantId, long amountMinor,
                                long feeMinor, String currency) {
    }

    public record SettlementResult(String batchId, String entityId, String currency, LocalDate date, int lines,
                                   long grossMinor, long feesMinor, long netMinor, String ledgerTransactionId) {
    }

    private final PaymentRepository repo;
    private final AccountsClient accounts;
    private final PricingClient pricing;
    private final SimulatorClient network;
    private final LedgerClient ledger;
    private final OutboxPublisher outbox;
    private final TransactionTemplate tx;
    private final long disputeFeeMinor;

    public PaymentService(PaymentRepository repo, AccountsClient accounts, PricingClient pricing, SimulatorClient network,
                          LedgerClient ledger, OutboxPublisher outbox, TransactionTemplate tx,
                          @Value("${payments.dispute-fee-minor:150000}") long disputeFeeMinor) {
        this.repo = repo;
        this.accounts = accounts;
        this.pricing = pricing;
        this.network = network;
        this.ledger = ledger;
        this.outbox = outbox;
        this.tx = tx;
        this.disputeFeeMinor = disputeFeeMinor;
    }

    /** Creates the intent and asks the network to authorize it. No money moves yet, so nothing is posted. */
    public PaymentIntent create(CreatePaymentRequest request) {
        AccountsClient.Merchant merchant = accounts.merchant(request.merchantId());
        if (!"ACTIVE".equals(merchant.status())) {
            throw new BusinessRuleException("MERCHANT_NOT_ACTIVE", "Merchant " + request.merchantId() + " is not active");
        }
        if (!merchant.settlementCurrency().equals(request.currency())) {
            throw new BusinessRuleException("CURRENCY_NOT_SUPPORTED",
                    "This merchant accepts " + merchant.settlementCurrency() + "; convert first via a wallet FX conversion");
        }
        String id = Ids.newId("pi");
        SimulatorClient.Authorization auth = network.authorize(id, request.amountMinor(), request.currency(), request.method(), request.instrument());
        String status = "APPROVED".equals(auth.status()) ? PaymentStatus.AUTHORIZED.name() : PaymentStatus.DECLINED.name();
        PaymentIntent intent = new PaymentIntent(id, merchant.entityId(), request.merchantId(), request.amountMinor(), request.currency(),
                request.method(), status, auth.networkRef(), auth.reason(), 0, 0, 0, 0, 0, null, request.description(), Instant.now(), Instant.now());
        repo.insert(intent);
        return get(id);
    }

    /**
     * Capture: the merchant confirms the sale. Ledger entry:
     * Dr 1100 Settlement receivable (gross) / Cr 2000 Merchant payable (net) / Cr 4000 Fee revenue / Cr 25xx Tax payable.
     */
    public PaymentIntent capture(String paymentId) {
        PaymentIntent p = get(paymentId);
        if (p.state() == PaymentStatus.CAPTURED) {
            return p;
        }
        p.state().moveTo(PaymentStatus.CAPTURED);
        Quote quote = pricing.quoteAndConsume(p.entityId(), "CARD_PAYMENT", p.amountMinor(), p.currency(), "payment:" + p.id());
        network.capture(p.networkRef(), p.amountMinor());

        Posting posting = Posting.of(p.entityId(), "PAYMENT_CAPTURE", "payment:" + p.id() + ":capture")
                .description("Capture of " + p.id()).reference("payment_intent", p.id())
                .debit("1100", p.amountMinor(), p.currency())
                .credit("2000:" + p.merchantId(), quote.netToRecipientMinor(), p.currency())
                .credit("4000", quote.totalFeesMinor(), p.currency());
        for (Quote.Tax tax : quote.taxes()) {
            posting.credit(tax.account(), tax.amountMinor(), p.currency());
        }
        ledger.post(posting);

        tx.executeWithoutResult(s -> {
            repo.find(paymentId, true).orElseThrow();
            repo.markCaptured(paymentId, p.amountMinor(), quote.totalFeesMinor(), quote.totalTaxesMinor(), quote.netToRecipientMinor(),
                    quote.quoteId());
            outbox.publish(Topics.PAYMENT_CAPTURED, p.merchantId(), "PaymentCaptured", new PaymentCaptured(p.id(), p.entityId(),
                    p.merchantId(), p.amountMinor(), p.currency(), quote.totalFeesMinor(), quote.totalTaxesMinor(),
                    quote.netToRecipientMinor(), Instant.now()));
        });
        return get(paymentId);
    }

    public PaymentIntent cancel(String paymentId) {
        PaymentIntent p = get(paymentId);
        repo.updateStatus(paymentId, p.state().moveTo(PaymentStatus.CANCELED).name());
        return get(paymentId);
    }

    /**
     * Refund: the merchant gives money back; the network collects it at the next settlement.
     * Dr 2000 Merchant payable / Cr 1100 Settlement receivable. The platform keeps its fee.
     * The merchant balance may go negative; collections recovers it.
     */
    public PaymentIntent refund(String paymentId, RefundRequest request) {
        PaymentIntent p = get(paymentId);
        long amount = request.amountMinor() <= 0 ? p.refundableMinor() : request.amountMinor();
        if (amount > p.refundableMinor()) {
            throw new BusinessRuleException("REFUND_TOO_LARGE", "Only " + p.refundableMinor() + " minor units can be refunded");
        }
        PaymentStatus next = amount == p.refundableMinor() ? PaymentStatus.REFUNDED : PaymentStatus.PARTIALLY_REFUNDED;
        p.state().moveTo(next);
        String refundId = Ids.newId("re");
        network.refund(p.networkRef(), amount);
        String txnId = ledger.post(Posting.of(p.entityId(), "PAYMENT_REFUND", "refund:" + refundId)
                .description("Refund of " + p.id()).reference("payment_intent", p.id())
                .debit("2000:" + p.merchantId(), amount, p.currency())
                .credit("1100", amount, p.currency())).transactionId();
        tx.executeWithoutResult(s -> {
            repo.find(paymentId, true).orElseThrow();
            repo.insertRefund(new Refund(refundId, paymentId, amount, request.reason(), txnId));
            repo.addRefunded(paymentId, amount, next.name());
            outbox.publish(Topics.PAYMENT_REFUNDED, p.merchantId(), "PaymentRefunded",
                    new PaymentRefunded(paymentId, p.entityId(), p.merchantId(), amount, p.currency()));
        });
        return get(paymentId);
    }

    /**
     * Chargeback: the customer's bank disputes the payment and the network pulls the money.
     * 1) Dr 2000 Merchant payable (amount + fee) / Cr 2200 Disputed funds held / Cr 4200 Dispute fee revenue
     * 2) Dr 2200 Disputed funds held / Cr 1100 Settlement receivable
     */
    public Dispute openDispute(String paymentId, DisputeRequest request) {
        PaymentIntent p = get(paymentId);
        p.state().moveTo(PaymentStatus.DISPUTED);
        long amount = p.refundableMinor();
        String disputeId = Ids.newId("dp");
        network.chargeback(p.networkRef(), amount, request.reason());
        ledger.post(Posting.of(p.entityId(), "DISPUTE_OPENED", "dispute:" + disputeId + ":hold")
                .description("Dispute " + disputeId + " on " + p.id()).reference("dispute", disputeId)
                .debit("2000:" + p.merchantId(), amount + disputeFeeMinor, p.currency())
                .credit("2200", amount, p.currency())
                .credit("4200", disputeFeeMinor, p.currency()));
        ledger.post(Posting.of(p.entityId(), "DISPUTE_FUNDS_PULLED", "dispute:" + disputeId + ":pull")
                .description("Network pulled disputed funds").reference("dispute", disputeId)
                .debit("2200", amount, p.currency())
                .credit("1100", amount, p.currency()));
        Dispute dispute = new Dispute(disputeId, paymentId, amount, disputeFeeMinor, "OPEN", request.reason());
        tx.executeWithoutResult(s -> {
            repo.insertDispute(dispute);
            repo.updateStatus(paymentId, PaymentStatus.DISPUTED.name());
            outbox.publish(Topics.DISPUTE_OPENED, p.merchantId(), "DisputeOpened", new DisputeOpened(disputeId, paymentId,
                    p.entityId(), p.merchantId(), amount, disputeFeeMinor, p.currency()));
        });
        return dispute;
    }

    /** WON: the network returns the money (Dr 1100 / Cr 2000). LOST: nothing more to post. The fee stays either way. */
    public Dispute resolveDispute(String disputeId, String outcome) {
        Dispute d = repo.dispute(disputeId, false).orElseThrow(() -> new NotFoundException("Dispute", disputeId));
        if (!"OPEN".equals(d.status())) {
            return d;
        }
        PaymentIntent p = get(d.paymentId());
        if ("WON".equalsIgnoreCase(outcome)) {
            network.chargebackReversal(p.networkRef(), d.amountMinor());
            ledger.post(Posting.of(p.entityId(), "DISPUTE_WON", "dispute:" + disputeId + ":won")
                    .description("Dispute won").reference("dispute", disputeId)
                    .debit("1100", d.amountMinor(), p.currency())
                    .credit("2000:" + p.merchantId(), d.amountMinor(), p.currency()));
        } else if (!"LOST".equalsIgnoreCase(outcome)) {
            throw new BusinessRuleException("INVALID_OUTCOME", "outcome must be WON or LOST");
        }
        String finalStatus = "WON".equalsIgnoreCase(outcome) ? "WON" : "LOST";
        tx.executeWithoutResult(s -> {
            repo.resolveDispute(disputeId, finalStatus);
            repo.updateStatus(p.id(), p.state().moveTo("WON".equals(finalStatus) ? PaymentStatus.CAPTURED : PaymentStatus.CHARGED_BACK).name());
        });
        return repo.dispute(disputeId, false).orElseThrow();
    }

    /**
     * Settlement: the network pays what it owes, net of its fees.
     * Dr 1000 Cash (net) / Dr 5000 Network fees / Cr 1100 Settlement receivable (gross).
     * The batch id is the reference reconciliation uses to match the bank statement.
     */
    public SettlementResult settle(SettlementRequest request) {
        LocalDate date = request.date() != null ? request.date() : LocalDate.now();
        SimulatorClient.SettlementBatch batch = network.settle(request.currency(), date);
        String txnId = null;
        if (!batch.lines().isEmpty()) {
            Posting posting = Posting.of(request.entityId(), "NETWORK_SETTLEMENT", "settlement:" + batch.batchId())
                    .description("Network settlement " + batch.batchId()).reference("settlement_batch", batch.batchId())
                    .effectiveDate(batch.date().toString());
            if (batch.netMinor() > 0) {
                posting.debit("1000", batch.netMinor(), request.currency());
            } else if (batch.netMinor() < 0) {
                posting.credit("1000", -batch.netMinor(), request.currency());
            }
            posting.debit("5000", batch.feesMinor(), request.currency());
            if (batch.grossMinor() > 0) {
                posting.credit("1100", batch.grossMinor(), request.currency());
            } else if (batch.grossMinor() < 0) {
                posting.debit("1100", -batch.grossMinor(), request.currency());
            }
            txnId = ledger.post(posting).transactionId();
        }
        String ledgerTxnId = txnId;
        tx.executeWithoutResult(s -> repo.insertSettlement(batch.batchId(), request.entityId(), request.currency(), batch.date(),
                batch.grossMinor(), batch.feesMinor(), batch.netMinor(), batch.lines().size(), ledgerTxnId));
        return new SettlementResult(batch.batchId(), request.entityId(), request.currency(), batch.date(), batch.lines().size(),
                batch.grossMinor(), batch.feesMinor(), batch.netMinor(), ledgerTxnId);
    }

    public PaymentIntent get(String paymentId) {
        return repo.find(paymentId, false).orElseThrow(() -> new NotFoundException("Payment intent", paymentId));
    }

    public List<PaymentIntent> byMerchant(String merchantId) {
        return repo.byMerchant(merchantId, 100);
    }

    public List<Refund> refunds(String paymentId) {
        return repo.refunds(paymentId);
    }

    public List<Map<String, Object>> settlements(String entityId) {
        return repo.settlements(entityId);
    }
}
