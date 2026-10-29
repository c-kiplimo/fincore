package com.collins.platform.accounts.application;

import com.collins.platform.accounts.infrastructure.AccountsRepository;
import com.collins.platform.accounts.infrastructure.AccountsRepository.Merchant;
import com.collins.platform.accounts.infrastructure.AccountsRepository.ReserveHold;
import com.collins.platform.commons.events.EventEnvelope;
import com.collins.platform.commons.events.EventReader;
import com.collins.platform.commons.events.ProcessedEvents;
import com.collins.platform.commons.events.Topics;
import com.collins.platform.commons.exception.BusinessRuleException;
import com.collins.platform.commons.exception.NotFoundException;
import com.collins.platform.commons.jdbc.Ids;
import com.collins.platform.commons.money.CurrencyUnits;
import com.collins.platform.ledger.client.LedgerBalance;
import com.collins.platform.ledger.client.LedgerClient;
import com.collins.platform.ledger.client.Posting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Merchants module: onboarding, balances, and rolling reserves. */
@Service
public class MerchantService {

    private static final Logger log = LoggerFactory.getLogger(MerchantService.class);

    public record MerchantBalances(String merchantId, String currency, long payableMinor, long reserveMinor,
                                   long availableForPayoutMinor) {
    }

    public record PaymentCaptured(String paymentId, String entityId, String merchantId, long amountMinor, String currency,
                                  long feeMinor, long taxMinor, long netMinor) {
    }

    private final AccountsRepository repo;
    private final LedgerClient ledger;
    private final ProcessedEvents processed;
    private final EventReader events;
    private final TransactionTemplate tx;

    public MerchantService(AccountsRepository repo, LedgerClient ledger, ProcessedEvents processed, EventReader events,
                           TransactionTemplate tx) {
        this.repo = repo;
        this.ledger = ledger;
        this.processed = processed;
        this.events = events;
        this.tx = tx;
    }

    public Merchant onboard(String entityId, String name, String country, String currency, int reservePercentBp, int reserveDays) {
        if (!CurrencyUnits.supported(currency)) {
            throw new BusinessRuleException("UNSUPPORTED_CURRENCY", "Unsupported currency " + currency);
        }
        if (reservePercentBp < 0 || reservePercentBp > 10_000 || reserveDays < 0) {
            throw new BusinessRuleException("INVALID_RESERVE", "Reserve percent must be 0-10000 bp and days >= 0");
        }
        Merchant m = new Merchant(Ids.newId("mer"), entityId, name, country, currency, reservePercentBp, reserveDays, "ACTIVE");
        repo.insertMerchant(m);
        return m;
    }

    public Merchant get(String merchantId) {
        return repo.merchant(merchantId).orElseThrow(() -> new NotFoundException("Merchant", merchantId));
    }

    public List<Merchant> list(String entityId) {
        return repo.merchants(entityId);
    }

    public MerchantBalances balances(String merchantId) {
        Merchant m = get(merchantId);
        LedgerBalance payable = ledger.balance(m.entityId(), "2000:" + m.merchantId(), m.settlementCurrency());
        LedgerBalance reserve = ledger.balance(m.entityId(), "2100:" + m.merchantId(), m.settlementCurrency());
        return new MerchantBalances(merchantId, m.settlementCurrency(), payable.availableMinor(), reserve.availableMinor(),
                Math.max(0, payable.availableMinor()));
    }

    public List<ReserveHold> holds(String merchantId) {
        return repo.holds(merchantId);
    }

    /**
     * Rolling reserve: when a payment is captured for a merchant with a reserve rule, move that share of the
     * net amount from Merchant payable (2000) to Merchant reserve (2100). Idempotent per event and per payment.
     */
    @KafkaListener(topics = Topics.PAYMENT_CAPTURED, groupId = "accounts-service-reserves")
    public void onPaymentCaptured(String message) {
        EventEnvelope envelope = events.read(message);
        PaymentCaptured event = events.payload(envelope, PaymentCaptured.class);
        Merchant m = repo.merchant(event.merchantId()).orElse(null);
        if (m == null || m.reservePercentBp() == 0 || m.reserveDays() == 0) {
            processed.markProcessed("reserves", envelope.meta().eventId());
            return;
        }
        long reserve = CurrencyUnits.applyRate(event.netMinor(),
                BigDecimal.valueOf(m.reservePercentBp()).divide(BigDecimal.valueOf(10_000)));
        if (reserve <= 0) {
            return;
        }
        ledger.post(Posting.of(m.entityId(), "RESERVE_HOLD", "reserve:" + event.paymentId() + ":hold")
                .description("Rolling reserve for " + event.paymentId()).reference("payment_intent", event.paymentId())
                .debit("2000:" + m.merchantId(), reserve, event.currency())
                .credit("2100:" + m.merchantId(), reserve, event.currency()));
        tx.executeWithoutResult(s -> {
            if (processed.markProcessed("reserves", envelope.meta().eventId())) {
                repo.insertHold(new ReserveHold(Ids.newId("rh"), m.merchantId(), event.paymentId(), m.entityId(), reserve,
                        event.currency(), LocalDate.now().plusDays(m.reserveDays()), "HELD"));
            }
        });
        log.info("Held {} {} in reserve for merchant {}", reserve, event.currency(), m.merchantId());
    }

    /** Releases reserves that are due: Dr 2100 Merchant reserve / Cr 2000 Merchant payable. */
    public int releaseDue(LocalDate asOf) {
        int released = 0;
        for (ReserveHold h : repo.dueHolds(asOf)) {
            ledger.post(Posting.of(h.entityId(), "RESERVE_RELEASE", "reserve:" + h.paymentId() + ":release")
                    .description("Reserve release for " + h.paymentId()).reference("payment_intent", h.paymentId())
                    .debit("2100:" + h.merchantId(), h.amountMinor(), h.currency())
                    .credit("2000:" + h.merchantId(), h.amountMinor(), h.currency()));
            if (repo.markReleased(h.holdId())) {
                released++;
            }
        }
        return released;
    }

    @Scheduled(cron = "${accounts.reserve-release-cron:0 0 2 * * *}")
    public void scheduledRelease() {
        int n = releaseDue(LocalDate.now());
        if (n > 0) {
            log.info("Released {} reserve holds", n);
        }
    }
}
