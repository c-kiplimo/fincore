package com.collins.platform.ledger.application;

import com.collins.platform.ledger.infrastructure.ReportQueries;
import com.collins.platform.ledger.infrastructure.ReportQueries.AccountTotals;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Trial balance, balance sheet and income statement, built from posted entries.
 * Amounts are per currency: the ledger never adds different currencies together.
 */
@Service
public class ReportService {

    public static final String ASSET = "ASSET";
    private static final LocalDate BEGINNING = LocalDate.of(1900, 1, 1);
    private static final Set<String> BALANCE_SHEET = Set.of("ASSET", "CONTRA_ASSET", "LIABILITY", "EQUITY");
    private final ReportQueries queries;

    public ReportService(ReportQueries queries) {
        this.queries = queries;
    }

    public TrialBalance trialBalance(String entityId, LocalDate asOf) {
        List<TrialBalanceRow> rows = new ArrayList<>();
        Map<String, long[]> byCurrency = new LinkedHashMap<>();
        for (AccountTotals t : queries.totals(entityId, BEGINNING, asOf)) {
            long net = t.net();
            if (net == 0) {
                continue;
            }
            long debit = net > 0 ? net : 0;
            long credit = net < 0 ? -net : 0;
            rows.add(new TrialBalanceRow(t.account(), t.name(), t.accountType(), t.currency(), debit, credit));
            long[] sums = byCurrency.computeIfAbsent(t.currency(), c -> new long[2]);
            sums[0] += debit;
            sums[1] += credit;
        }
        List<CurrencyTotal> totals = byCurrency.entrySet().stream()
                .map(e -> new CurrencyTotal(e.getKey(), e.getValue()[0], e.getValue()[1], e.getValue()[0] == e.getValue()[1]))
                .toList();
        return new TrialBalance(entityId, asOf, rows, totals, totals.stream().allMatch(CurrencyTotal::balanced));
    }

    public BalanceSheet balanceSheet(String entityId, LocalDate asOf, String currency) {
        List<Line> assets = new ArrayList<>();
        List<Line> liabilities = new ArrayList<>();
        List<Line> equity = new ArrayList<>();
        long totalAssets = 0, totalLiabilities = 0, totalEquity = 0, profit = 0;
        for (AccountTotals t : queries.totals(entityId, BEGINNING, asOf)) {
            if (!t.currency().equals(currency)) {
                continue;
            }
            switch (t.accountType()) {
                case ASSET -> { assets.add(new Line(t.account(), t.name(), t.natural())); totalAssets += t.natural(); }
                case "CONTRA_ASSET" -> { assets.add(new Line(t.account(), t.name(), -t.natural())); totalAssets -= t.natural(); }
                case "LIABILITY" -> { liabilities.add(new Line(t.account(), t.name(), t.natural())); totalLiabilities += t.natural(); }
                case "EQUITY" -> { equity.add(new Line(t.account(), t.name(), t.natural())); totalEquity += t.natural(); }
                case "REVENUE" -> profit += t.natural();
                case "EXPENSE" -> profit -= t.natural();
                default -> { }
            }
        }
        long equityWithProfit = totalEquity + profit;
        return new BalanceSheet(entityId, asOf, currency, assets, totalAssets, liabilities, totalLiabilities, equity, profit,
                equityWithProfit, totalAssets == totalLiabilities + equityWithProfit);
    }

    public IncomeStatement incomeStatement(String entityId, LocalDate from, LocalDate to, String currency) {
        List<Line> revenue = new ArrayList<>();
        List<Line> expenses = new ArrayList<>();
        long totalRevenue = 0, totalExpenses = 0;
        for (AccountTotals t : queries.totals(entityId, from, to, true)) {
            if (!t.currency().equals(currency) || BALANCE_SHEET.contains(t.accountType())) {
                continue;
            }
            if ("REVENUE".equals(t.accountType())) {
                revenue.add(new Line(t.account(), t.name(), t.natural()));
                totalRevenue += t.natural();
            } else if ("EXPENSE".equals(t.accountType())) {
                expenses.add(new Line(t.account(), t.name(), t.natural()));
                totalExpenses += t.natural();
            }
        }
        return new IncomeStatement(entityId, from, to, currency, revenue, totalRevenue, expenses, totalExpenses, totalRevenue - totalExpenses);
    }

    public record TrialBalanceRow(String account, String name, String accountType, String currency,
                                  long debitBalanceMinor, long creditBalanceMinor) {
    }

    public record CurrencyTotal(String currency, long totalDebitsMinor, long totalCreditsMinor, boolean balanced) {
    }

    public record TrialBalance(String entityId, LocalDate asOf, List<TrialBalanceRow> rows, List<CurrencyTotal> totals, boolean balanced) {
    }

    public record Line(String account, String name, long amountMinor) {
    }

    public record BalanceSheet(String entityId, LocalDate asOf, String currency,
                               List<Line> assets, long totalAssetsMinor,
                               List<Line> liabilities, long totalLiabilitiesMinor,
                               List<Line> equity, long currentPeriodProfitMinor, long totalEquityMinor,
                               boolean balanced) {
    }

    public record IncomeStatement(String entityId, LocalDate from, LocalDate to, String currency,
                                  List<Line> revenue, long totalRevenueMinor,
                                  List<Line> expenses, long totalExpensesMinor, long profitMinor) {
    }
}
