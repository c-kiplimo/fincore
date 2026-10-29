package com.collins.platform.ledger.application;

import com.collins.platform.commons.exception.BusinessRuleException;
import com.collins.platform.ledger.infrastructure.LedgerRepository;
import com.collins.platform.ledger.infrastructure.LedgerRows.ChartAccount;
import com.collins.platform.ledger.infrastructure.ReportQueries;
import com.collins.platform.ledger.infrastructure.ReportQueries.StatementLine;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class BalanceService {

    public record StatementRow(String transactionId, LocalDate effectiveDate, String transactionType, String description,
                               String direction, long amountMinor, long runningBalanceMinor) {
    }

    public record Statement(String entityId, String account, String currency, LocalDate from, LocalDate to,
                            long openingBalanceMinor, long closingBalanceMinor, List<StatementRow> lines) {
    }

    private final LedgerRepository repo;
    private final ReportQueries reports;

    public BalanceService(LedgerRepository repo, ReportQueries reports) {
        this.repo = repo;
        this.reports = reports;
    }

    /** Current balance from the projection (fast path used by gRPC). */
    public BalanceView current(String entityId, String account, String currency) {
        ChartAccount chart = chart(entityId, account);
        long[] b = repo.currentBalance(entityId, account, currency, chart.normalSide());
        return new BalanceView(entityId, account, currency, b[0], b[1], b[0] + b[1]);
    }

    /** Balance at any past date, computed from posted entries (the source of truth). */
    public BalanceView asOf(String entityId, String account, String currency, LocalDate asOf) {
        ChartAccount chart = chart(entityId, account);
        long natural = naturalBetween(entityId, account, currency, LocalDate.of(1900, 1, 1), asOf, chart.normalSide());
        return new BalanceView(entityId, account, currency, natural, 0, natural);
    }

    public Statement statement(String entityId, String account, String currency, LocalDate from, LocalDate to) {
        ChartAccount chart = chart(entityId, account);
        long opening = naturalBetween(entityId, account, currency, LocalDate.of(1900, 1, 1), from.minusDays(1), chart.normalSide());
        long running = opening;
        List<StatementRow> rows = new ArrayList<>();
        for (StatementLine line : reports.statement(entityId, account, currency, from, to)) {
            long signed = "DEBIT".equals(line.direction()) ? line.amountMinor() : -line.amountMinor();
            running += "DEBIT".equals(chart.normalSide()) ? signed : -signed;
            rows.add(new StatementRow(line.transactionId(), line.effectiveDate(), line.transactionType(), line.description(),
                    line.direction(), line.amountMinor(), running));
        }
        return new Statement(entityId, account, currency, from, to, opening, running, rows);
    }

    private long naturalBetween(String entityId, String account, String currency, LocalDate from, LocalDate to, String normalSide) {
        long net = 0;
        for (StatementLine line : reports.statement(entityId, account, currency, from, to)) {
            net += "DEBIT".equals(line.direction()) ? line.amountMinor() : -line.amountMinor();
        }
        return "DEBIT".equals(normalSide) ? net : -net;
    }

    private ChartAccount chart(String entityId, String account) {
        String base = account.contains(":") ? account.substring(0, account.indexOf(':')) : account;
        return repo.chartAccount(entityId, base)
                .orElseThrow(() -> new BusinessRuleException("UNKNOWN_ACCOUNT", "Account " + base + " is not in the chart of " + entityId));
    }
}
