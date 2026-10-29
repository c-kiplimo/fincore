package com.collins.platform.ledger.application;

import com.collins.platform.commons.exception.BusinessRuleException;
import com.collins.platform.commons.exception.NotFoundException;
import com.collins.platform.commons.money.CurrencyUnits;
import com.collins.platform.ledger.domain.Direction;
import com.collins.platform.ledger.domain.EntryCommand;
import com.collins.platform.ledger.domain.PostingCommand;
import com.collins.platform.ledger.infrastructure.LedgerRepository;
import com.collins.platform.ledger.infrastructure.LedgerRows.AccountRef;
import com.collins.platform.ledger.infrastructure.LedgerRows.EntityRow;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Month-end revaluation (IAS 21 / ASC 830): foreign-currency monetary balances are re-measured in the
 * entity's functional currency at the closing rate. The change since the last rate is an unrealized
 * FX gain (4900) or loss (5200), booked against 1410 FX revaluation adjustment.
 * The first run for an account only records the baseline rate.
 */
@Service
public class RevaluationService {

    public record AccountRevaluation(String account, String currency, long balanceMinor, BigDecimal previousRate,
                                     BigDecimal newRate, long adjustmentFunctionalMinor) {
    }

    public record RevaluationResult(String entityId, LocalDate asOf, String functionalCurrency,
                                    List<AccountRevaluation> accounts, String transactionId) {
    }

    private final LedgerRepository repo;
    private final BalanceService balances;
    private final PostingService posting;

    public RevaluationService(LedgerRepository repo, BalanceService balances, PostingService posting) {
        this.repo = repo;
        this.balances = balances;
        this.posting = posting;
    }

    @Transactional
    public RevaluationResult revalue(String entityId, LocalDate asOf, Map<String, BigDecimal> ratesToFunctional) {
        EntityRow entity = repo.entity(entityId).orElseThrow(() -> new NotFoundException("Legal entity", entityId));
        String functional = entity.functionalCurrency();
        List<AccountRevaluation> results = new ArrayList<>();
        List<EntryCommand> entries = new ArrayList<>();
        long gain = 0;
        long loss = 0;

        for (AccountRef ref : repo.foreignCurrencyMonetaryAccounts(entityId, functional)) {
            BigDecimal rate = ratesToFunctional.get(ref.currency());
            if (rate == null) {
                throw new BusinessRuleException("RATE_MISSING", "No rate given for " + ref.currency() + " -> " + functional);
            }
            long balance = balances.asOf(entityId, ref.account(), ref.currency(), asOf).postedMinor();
            BigDecimal previous = repo.lastRevaluationRate(ref.id()).orElse(null);
            long adjustment = 0;
            if (previous != null && balance != 0) {
                long before = CurrencyUnits.convert(balance, ref.currency(), previous, functional);
                long after = CurrencyUnits.convert(balance, ref.currency(), rate, functional);
                adjustment = after - before; // change in what the balance is worth, in functional currency
                boolean isAsset = "DEBIT".equals(ref.normalSide());
                long effect = isAsset ? adjustment : -adjustment; // a bigger liability is a loss
                if (effect > 0) {
                    gain += effect;
                } else {
                    loss += -effect;
                }
            }
            repo.saveRevaluationRate(ref.id(), rate, asOf);
            results.add(new AccountRevaluation(ref.account(), ref.currency(), balance, previous, rate, adjustment));
        }

        String transactionId = null;
        long net = gain - loss;
        if (net != 0) {
            if (net > 0) {
                entries.add(new EntryCommand("1410", Direction.DEBIT, net, functional, false));
                entries.add(new EntryCommand("4900", Direction.CREDIT, net, functional, false));
            } else {
                entries.add(new EntryCommand("5200", Direction.DEBIT, -net, functional, false));
                entries.add(new EntryCommand("1410", Direction.CREDIT, -net, functional, false));
            }
            transactionId = posting.post(new PostingCommand("reval:" + entityId + ":" + asOf, entityId, "FX_REVALUATION",
                    "Unrealized FX revaluation at " + asOf, asOf, "revaluation", asOf.toString(), false, entries, Map.of())).transactionId();
        }
        return new RevaluationResult(entityId, asOf, functional, results, transactionId);
    }
}
