package com.collins.platform.accounts.application;

import com.collins.platform.accounts.infrastructure.AccountsRepository;
import com.collins.platform.accounts.infrastructure.AccountsRepository.Individual;
import com.collins.platform.accounts.infrastructure.AccountsRepository.Merchant;
import com.collins.platform.accounts.infrastructure.AccountsRepository.Transfer;
import com.collins.platform.accounts.infrastructure.AccountsRepository.Wallet;
import com.collins.platform.commons.clients.PricingClient;
import com.collins.platform.commons.clients.Quote;
import com.collins.platform.commons.exception.BusinessRuleException;
import com.collins.platform.commons.exception.NotFoundException;
import com.collins.platform.commons.jdbc.Ids;
import com.collins.platform.commons.money.CurrencyUnits;
import com.collins.platform.ledger.client.LedgerClient;
import com.collins.platform.ledger.client.Posting;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Wallets module. Wallet money is a liability (2010): the platform owes it to the person.
 * Moving money between wallets moves no cash; only who the platform owes changes.
 */
@Service
public class WalletService {

    public record WalletView(String walletId, String individualId, String entityId, String currency, String kind,
                             String ledgerAccount, Long availableMinor) {
    }

    private final AccountsRepository repo;
    private final LedgerClient ledger;
    private final PricingClient pricing;
    private final MerchantService merchants;

    public WalletService(AccountsRepository repo, LedgerClient ledger, PricingClient pricing, MerchantService merchants) {
        this.repo = repo;
        this.ledger = ledger;
        this.pricing = pricing;
        this.merchants = merchants;
    }

    public Individual register(String entityId, String name, String country, String mainCurrency) {
        Individual ind = new Individual(Ids.newId("ind"), entityId, name, country);
        repo.insertIndividual(ind);
        openWallet(ind.individualId(), mainCurrency, "MAIN");
        return ind;
    }

    public Individual individual(String id) {
        return repo.individual(id).orElseThrow(() -> new NotFoundException("Individual", id));
    }

    public WalletView openWallet(String individualId, String currency, String kind) {
        Individual ind = individual(individualId);
        if (!CurrencyUnits.supported(currency)) {
            throw new BusinessRuleException("UNSUPPORTED_CURRENCY", "Unsupported currency " + currency);
        }
        String id = Ids.newId("wal");
        Wallet w = new Wallet(id, ind.individualId(), ind.entityId(), currency, kind, "2010:" + id, "ACTIVE");
        repo.insertWallet(w);
        return view(w);
    }

    public WalletView wallet(String walletId) {
        return view(walletRow(walletId));
    }

    public List<WalletView> wallets(String individualId) {
        individual(individualId);
        return repo.wallets(individualId).stream().map(this::view).toList();
    }

    /** Top-up by card or mobile money: Dr 1100 Settlement receivable / Cr 2010 Customer wallet. */
    public Transfer topUp(String walletId, long amountMinor, String method) {
        Wallet w = walletRow(walletId);
        String id = Ids.newId("tr");
        String txn = ledger.post(Posting.of(w.entityId(), "WALLET_TOP_UP", "topup:" + id)
                .description("Top-up via " + method).reference("transfer", id)
                .debit("1100", amountMinor, w.currency())
                .credit(w.ledgerAccount(), amountMinor, w.currency())).transactionId();
        return save(new Transfer(id, "TOP_UP", w.entityId(), method, walletId, amountMinor, w.currency(), 0, 0, null, null, null, txn));
    }

    /**
     * Transfer between wallets in the same currency. Own accounts: no fee. To another person (P2P):
     * Dr sender wallet (amount + fee + tax) / Cr receiver wallet (amount) / Cr 4300 fee / Cr 25xx tax.
     */
    public Transfer transfer(String fromWalletId, String toWalletId, long amountMinor) {
        Wallet from = walletRow(fromWalletId);
        Wallet to = walletRow(toWalletId);
        if (!from.currency().equals(to.currency())) {
            throw new BusinessRuleException("CURRENCY_MISMATCH", "Use /v1/conversions to move money between currencies");
        }
        if (!from.entityId().equals(to.entityId())) {
            throw new BusinessRuleException("CROSS_ENTITY_TRANSFER", "Transfers across legal entities are not supported yet");
        }
        String id = Ids.newId("tr");
        boolean own = from.individualId().equals(to.individualId());
        if (own) {
            String txn = ledger.post(Posting.of(from.entityId(), "WALLET_OWN_TRANSFER", "transfer:" + id)
                    .description("Move between own wallets").reference("transfer", id)
                    .debitNoOverdraft(from.ledgerAccount(), amountMinor, from.currency())
                    .credit(to.ledgerAccount(), amountMinor, to.currency())).transactionId();
            return save(new Transfer(id, "OWN", from.entityId(), fromWalletId, toWalletId, amountMinor, from.currency(), 0, 0,
                    null, null, null, txn));
        }
        Quote quote = pricing.quoteAndConsume(from.entityId(), "P2P_TRANSFER", amountMinor, from.currency(), "transfer:" + id);
        Posting posting = Posting.of(from.entityId(), "P2P_TRANSFER", "transfer:" + id)
                .description("P2P transfer").reference("transfer", id)
                .debitNoOverdraft(from.ledgerAccount(), quote.totalDebitedMinor(), from.currency())
                .credit(to.ledgerAccount(), quote.netToRecipientMinor(), to.currency())
                .credit("4300", quote.totalFeesMinor(), from.currency());
        quote.taxes().forEach(t -> posting.credit(t.account(), t.amountMinor(), from.currency()));
        String txn = ledger.post(posting).transactionId();
        return save(new Transfer(id, "P2P", from.entityId(), fromWalletId, toWalletId, amountMinor, from.currency(),
                quote.totalFeesMinor(), quote.totalTaxesMinor(), null, null, quote.quoteId(), txn));
    }

    /**
     * Currency conversion between one person's wallets, joined by 1400 FX clearing in both currencies:
     * USD side: Dr USD wallet / Cr 1400 (USD). KES side: Dr 1400 at the mid rate / Cr KES wallet at the customer rate
     * / Cr 4500 FX spread revenue.
     */
    public Transfer convert(String fromWalletId, String toWalletId, long amountMinor) {
        Wallet from = walletRow(fromWalletId);
        Wallet to = walletRow(toWalletId);
        if (!from.individualId().equals(to.individualId())) {
            throw new BusinessRuleException("NOT_OWN_WALLETS", "Conversions are between one person's own wallets");
        }
        if (from.currency().equals(to.currency())) {
            throw new BusinessRuleException("SAME_CURRENCY", "Both wallets use " + from.currency() + "; use /v1/transfers");
        }
        String id = Ids.newId("tr");
        Quote quote = pricing.quote(from.entityId(), "FX_CONVERSION", amountMinor, from.currency(), to.currency(), null, null);
        quote = pricing.consume(quote.quoteId(), "conversion:" + id);
        Quote.Fx fx = quote.fx();
        String txn = ledger.post(Posting.of(from.entityId(), "FX_CONVERSION", "conversion:" + id)
                .description("Convert " + from.currency() + " to " + to.currency() + " at " + fx.customerRate())
                .reference("transfer", id)
                .meta("mid_rate", fx.midRate().toPlainString()).meta("customer_rate", fx.customerRate().toPlainString())
                .debitNoOverdraft(from.ledgerAccount(), amountMinor, from.currency())
                .credit("1400", amountMinor, from.currency())
                .debit("1400", fx.midConvertedMinor(), to.currency())
                .credit(to.ledgerAccount(), fx.convertedMinor(), to.currency())
                .credit("4500", fx.spreadRevenueMinor(), to.currency())).transactionId();
        return save(new Transfer(id, "CONVERSION", from.entityId(), fromWalletId, toWalletId, amountMinor, from.currency(), 0, 0,
                fx.convertedMinor(), to.currency(), quote.quoteId(), txn));
    }

    /** Paying a merchant from a wallet: Dr wallet / Cr 2000 Merchant payable (net) / Cr 4000 fee / Cr 25xx tax. */
    public Transfer payMerchant(String walletId, String merchantId, long amountMinor) {
        Wallet w = walletRow(walletId);
        Merchant m = merchants.get(merchantId);
        if (!m.settlementCurrency().equals(w.currency()) || !m.entityId().equals(w.entityId())) {
            throw new BusinessRuleException("CURRENCY_MISMATCH", "Wallet and merchant must share currency and legal entity");
        }
        String id = Ids.newId("tr");
        Quote quote = pricing.quoteAndConsume(w.entityId(), "WALLET_PAYMENT", amountMinor, w.currency(), "wallet-payment:" + id);
        Posting posting = Posting.of(w.entityId(), "WALLET_PAYMENT", "wallet-payment:" + id)
                .description("Wallet payment to " + merchantId).reference("transfer", id)
                .debitNoOverdraft(w.ledgerAccount(), quote.totalDebitedMinor(), w.currency())
                .credit("2000:" + merchantId, quote.netToRecipientMinor(), w.currency())
                .credit("4000", quote.totalFeesMinor(), w.currency());
        quote.taxes().forEach(t -> posting.credit(t.account(), t.amountMinor(), w.currency()));
        String txn = ledger.post(posting).transactionId();
        return save(new Transfer(id, "WALLET_PAYMENT", w.entityId(), walletId, merchantId, amountMinor, w.currency(),
                quote.totalFeesMinor(), quote.totalTaxesMinor(), null, null, quote.quoteId(), txn));
    }

    /** A merchant pays an individual directly (seller earnings, prize, refund to wallet): Dr 2000 / Cr 2010. */
    public Transfer disburseFromMerchant(String merchantId, String walletId, long amountMinor) {
        Wallet w = walletRow(walletId);
        Merchant m = merchants.get(merchantId);
        if (!m.settlementCurrency().equals(w.currency()) || !m.entityId().equals(w.entityId())) {
            throw new BusinessRuleException("CURRENCY_MISMATCH", "Wallet and merchant must share currency and legal entity");
        }
        String id = Ids.newId("tr");
        String txn = ledger.post(Posting.of(w.entityId(), "MERCHANT_DISBURSEMENT", "disbursement:" + id)
                .description("Payment from " + merchantId).reference("transfer", id)
                .debitNoOverdraft("2000:" + merchantId, amountMinor, w.currency())
                .credit(w.ledgerAccount(), amountMinor, w.currency())).transactionId();
        return save(new Transfer(id, "MERCHANT_DISBURSEMENT", w.entityId(), merchantId, walletId, amountMinor, w.currency(),
                0, 0, null, null, null, txn));
    }

    public List<Transfer> history(String ref) {
        return repo.transfers(ref);
    }

    private Transfer save(Transfer t) {
        repo.insertTransfer(t);
        return t;
    }

    private Wallet walletRow(String walletId) {
        return repo.wallet(walletId).orElseThrow(() -> new NotFoundException("Wallet", walletId));
    }

    private WalletView view(Wallet w) {
        long available = ledger.balance(w.entityId(), w.ledgerAccount(), w.currency()).availableMinor();
        return new WalletView(w.walletId(), w.individualId(), w.entityId(), w.currency(), w.kind(), w.ledgerAccount(), available);
    }
}
