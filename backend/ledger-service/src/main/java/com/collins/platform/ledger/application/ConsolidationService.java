package com.collins.platform.ledger.application;

import com.collins.platform.commons.exception.BusinessRuleException;
import com.collins.platform.commons.money.CurrencyUnits;
import com.collins.platform.ledger.infrastructure.ReportQueries;
import com.collins.platform.ledger.infrastructure.ReportQueries.AccountTotals;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Group consolidation: translate every entity's balances into the reporting currency, remove what the
 * group owes itself (intercompany), add them up, and put the translation difference in 3200.
 * Assets and liabilities use closing rates; revenue and expenses use average rates when given.
 */
@Service
public class ConsolidationService {

    private static final Set<String> INTERCOMPANY = Set.of("1600", "2600", "4600", "5600");

    public record Line(String code, String name, String accountType, long amountMinor) {
    }

    public record Elimination(String code, long amountMinor) {
    }

    public record ConsolidatedReport(List<String> entityIds, String reportingCurrency, LocalDate asOf,
                                     List<Line> lines, List<Elimination> eliminations,
                                     long totalAssetsMinor, long totalLiabilitiesMinor, long totalEquityMinor,
                                     long profitMinor, long translationReserveMinor, boolean intercompanyMatched) {
    }

    private final ReportQueries queries;

    public ConsolidationService(ReportQueries queries) {
        this.queries = queries;
    }

    public ConsolidatedReport consolidate(List<String> entityIds, String reportingCurrency, LocalDate asOf,
                                          Map<String, BigDecimal> closingRates, Map<String, BigDecimal> averageRates) {
        Map<String, long[]> byCode = new TreeMap<>(); // code -> natural amount in reporting currency
        Map<String, String[]> info = new TreeMap<>();
        for (String entityId : entityIds) {
            for (AccountTotals t : queries.totals(entityId, LocalDate.of(1900, 1, 1), asOf)) {
                boolean pnl = "REVENUE".equals(t.accountType()) || "EXPENSE".equals(t.accountType());
                BigDecimal rate = rate(t.currency(), reportingCurrency, pnl ? averageRates : closingRates, closingRates);
                long translated = CurrencyUnits.convert(t.natural(), t.currency(), rate, reportingCurrency);
                byCode.computeIfAbsent(t.baseCode(), c -> new long[1])[0] += translated;
                info.putIfAbsent(t.baseCode(), new String[]{t.name(), t.accountType()});
            }
        }

        List<Elimination> eliminations = new ArrayList<>();
        long icReceivable = 0, icPayable = 0, icRevenue = 0, icExpense = 0;
        for (String code : INTERCOMPANY) {
            long amount = byCode.containsKey(code) ? byCode.remove(code)[0] : 0;
            if (amount != 0) {
                eliminations.add(new Elimination(code, amount));
            }
            switch (code) {
                case "1600" -> icReceivable = amount;
                case "2600" -> icPayable = amount;
                case "4600" -> icRevenue = amount;
                default -> icExpense = amount;
            }
        }

        List<Line> lines = new ArrayList<>();
        long assets = 0, liabilities = 0, equity = 0, profit = 0;
        for (Map.Entry<String, long[]> e : byCode.entrySet()) {
            String[] meta = info.get(e.getKey());
            long amount = e.getValue()[0];
            lines.add(new Line(e.getKey(), meta[0], meta[1], amount));
            switch (meta[1]) {
                case "ASSET" -> assets += amount;
                case "CONTRA_ASSET" -> assets -= amount;
                case "LIABILITY" -> liabilities += amount;
                case "EQUITY" -> equity += amount;
                case "REVENUE" -> profit += amount;
                case "EXPENSE" -> profit -= amount;
                default -> { }
            }
        }
        // Whatever is left after translation is the translation reserve (equity), so the group balances.
        long translationReserve = assets - liabilities - equity - profit;
        return new ConsolidatedReport(entityIds, reportingCurrency, asOf, lines, eliminations, assets, liabilities,
                equity + translationReserve, profit, translationReserve,
                icReceivable == icPayable && icRevenue == icExpense);
    }

    private static BigDecimal rate(String from, String to, Map<String, BigDecimal> preferred, Map<String, BigDecimal> fallback) {
        if (from.equals(to)) {
            return BigDecimal.ONE;
        }
        BigDecimal r = preferred != null ? preferred.get(from) : null;
        if (r == null && fallback != null) {
            r = fallback.get(from);
        }
        if (r == null) {
            throw new BusinessRuleException("RATE_MISSING", "No rate given for " + from + " -> " + to);
        }
        return r;
    }
}
