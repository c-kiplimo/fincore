package com.collins.platform.reconciliation;

import java.time.LocalDate;
import java.util.List;

public final class ReconModels {

    private ReconModels() {
    }

    /** A line as the (simulated) bank sends it. */
    public record BankLine(String bankRef, LocalDate valueDate, String currency, long amountMinor, String description) {
    }

    public record LineInput(String externalRef, long amountMinor, LocalDate valueDate, String description) {
    }

    public record Run(String id, String entityId, String source, String currency, LocalDate statementDate, int matched, int exceptions) {
    }

    public record Line(String id, String runId, String externalRef, long amountMinor, String currency, LocalDate valueDate,
                       String description, String status, String matchedTxnId, String explanation, String suspenseTxnId) {
    }

    public record RunResult(Run run, List<Line> lines) {
    }

    public record Explanation(String lineId, String source, String explanation, String suggestedAction) {
    }

    public record Safeguarding(String entityId, String currency, long cashMinor, long settlementReceivableMinor,
                               long customerFundsMinor, long merchantFundsMinor, long surplusMinor, boolean covered) {
    }

    /** Subset of ledger-service's TransactionView that reconciliation needs. */
    public record LedgerTxn(String transactionId, String entityId, String status, String transactionType, String referenceId,
                            List<LedgerLine> entries) {
    }

    public record LedgerLine(String account, String direction, long amountMinor, String currency) {
    }

    public record LedgerBalance(String entityId, String account, String currency, long postedMinor, long pendingMinor, long availableMinor) {
    }
}
