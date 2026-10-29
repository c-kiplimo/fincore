package com.collins.platform.ledger.application;

import com.collins.platform.commons.exception.BusinessRuleException;
import com.collins.platform.commons.exception.ConflictException;
import com.collins.platform.ledger.domain.Direction;
import com.collins.platform.ledger.domain.EntryCommand;
import com.collins.platform.ledger.domain.PostingCommand;
import com.collins.platform.ledger.infrastructure.LedgerRepository;
import com.collins.platform.ledger.infrastructure.ReportQueries;
import com.collins.platform.ledger.infrastructure.ReportQueries.AccountTotals;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Month-end close. Closing entries move every revenue and expense balance into retained earnings
 * (3100), so the next period starts its income statement from zero; then the period is locked.
 */
@Service
public class PeriodService {

    public record CloseResult(String entityId, String period, List<String> closingTransactionIds, Map<String, Long> profitByCurrency) {
    }

    private final LedgerRepository repo;
    private final ReportQueries queries;
    private final ReportService reports;
    private final PostingService posting;

    public PeriodService(LedgerRepository repo, ReportQueries queries, ReportService reports, PostingService posting) {
        this.repo = repo;
        this.queries = queries;
        this.reports = reports;
        this.posting = posting;
    }

    @Transactional
    public CloseResult close(String entityId, YearMonth period) {
        if (repo.periodClosed(entityId, period.toString())) {
            throw new ConflictException("PERIOD_ALREADY_CLOSED", "Period " + period + " is already closed for " + entityId);
        }
        LocalDate end = period.atEndOfMonth();
        if (!reports.trialBalance(entityId, end).balanced()) {
            throw new BusinessRuleException("TRIAL_BALANCE_NOT_BALANCED", "Cannot close: the trial balance does not balance");
        }

        Map<String, List<EntryCommand>> entriesByCurrency = new LinkedHashMap<>();
        Map<String, Long> profitByCurrency = new LinkedHashMap<>();
        for (AccountTotals t : queries.totals(entityId, LocalDate.of(1900, 1, 1), end)) {
            if (!"REVENUE".equals(t.accountType()) && !"EXPENSE".equals(t.accountType())) {
                continue;
            }
            long net = t.net();
            if (net == 0) {
                continue;
            }
            // Bring the account to zero with the opposite side.
            Direction direction = net > 0 ? Direction.CREDIT : Direction.DEBIT;
            entriesByCurrency.computeIfAbsent(t.currency(), c -> new ArrayList<>())
                    .add(new EntryCommand(t.account(), direction, Math.abs(net), t.currency(), false));
            profitByCurrency.merge(t.currency(), -net, Long::sum); // revenue is credit (negative net) -> profit positive
        }

        List<String> ids = new ArrayList<>();
        for (Map.Entry<String, List<EntryCommand>> e : entriesByCurrency.entrySet()) {
            String currency = e.getKey();
            long profit = profitByCurrency.get(currency);
            List<EntryCommand> entries = new ArrayList<>(e.getValue());
            if (profit > 0) {
                entries.add(new EntryCommand("3100", Direction.CREDIT, profit, currency, false));
            } else if (profit < 0) {
                entries.add(new EntryCommand("3100", Direction.DEBIT, -profit, currency, false));
            }
            PostingCommand command = new PostingCommand("close:" + entityId + ":" + period + ":" + currency, entityId,
                    "PERIOD_CLOSE", "Closing entries for " + period, end, "period", period.toString(), false, entries, Map.of());
            ids.add(posting.post(command).transactionId());
        }
        repo.closePeriod(entityId, period.toString());
        return new CloseResult(entityId, period.toString(), ids, profitByCurrency);
    }

    public List<Map<String, Object>> periods(String entityId) {
        return repo.periods(entityId);
    }
}
