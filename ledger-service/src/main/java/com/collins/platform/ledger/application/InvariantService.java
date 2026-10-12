package com.collins.platform.ledger.application;

import com.collins.platform.ledger.infrastructure.ReportQueries;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** Continuous checks that prove the books are healthy. Alert when {@code healthy} is false. */
@Service
public class InvariantService {

    public record InvariantReport(String entityId, boolean trialBalanceBalanced, List<Map<String, Object>> projectionDrift,
                                  long stalePendingTransactions, boolean healthy) {
    }

    private final ReportService reports;
    private final ReportQueries queries;

    public InvariantService(ReportService reports, ReportQueries queries) {
        this.reports = reports;
        this.queries = queries;
    }

    public InvariantReport check(String entityId) {
        boolean balanced = reports.trialBalance(entityId, LocalDate.now().plusYears(100)).balanced();
        List<Map<String, Object>> drift = queries.projectionDrift(entityId);
        long stale = queries.stalePendingCount(entityId, 24);
        return new InvariantReport(entityId, balanced, drift, stale, balanced && drift.isEmpty() && stale == 0);
    }
}
