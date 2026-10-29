package com.collins.platform.reconciliation;

import com.collins.platform.commons.exception.BusinessRuleException;
import com.collins.platform.commons.exception.NotFoundException;
import com.collins.platform.commons.jdbc.Ids;
import com.collins.platform.reconciliation.ReconModels.Explanation;
import com.collins.platform.reconciliation.ReconModels.LedgerTxn;
import com.collins.platform.reconciliation.ReconModels.Line;
import com.collins.platform.reconciliation.ReconModels.LineInput;
import com.collins.platform.reconciliation.ReconModels.Run;
import com.collins.platform.reconciliation.ReconModels.RunResult;
import com.collins.platform.reconciliation.ReconModels.Safeguarding;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/**
 * Bank reconciliation: every statement line must be explained by a posted ledger transaction that moves the same
 * amount through cash (1000) with the same reference (settlement batch id or payout id). Unexplained lines become
 * exceptions; a person can park them in suspense (2900) and resolve them later.
 */
@Service
public class ReconciliationService {

    private final ReconRepository repo;
    private final LedgerRestClient ledger;
    private final MismatchExplainer explainer;

    public ReconciliationService(ReconRepository repo, LedgerRestClient ledger, MismatchExplainer explainer) {
        this.repo = repo;
        this.ledger = ledger;
        this.explainer = explainer;
    }

    public RunResult reconcile(String entityId, String source, String currency, LocalDate statementDate, List<LineInput> inputs) {
        Run run = new Run(Ids.newId("rr"), entityId, source, currency, statementDate, 0, 0);
        repo.insertRun(run);
        int matched = 0;
        int exceptions = 0;
        for (LineInput in : inputs) {
            String txnId = match(entityId, currency, in);
            String status = txnId != null ? "MATCHED" : "EXCEPTION";
            if (txnId != null) {
                matched++;
            } else {
                exceptions++;
            }
            repo.insertLine(new Line(Ids.newId("sl"), run.id(), in.externalRef(), in.amountMinor(), currency,
                    in.valueDate() == null ? statementDate : in.valueDate(), in.description(), status, txnId, null, null));
        }
        repo.updateCounts(run.id(), matched, exceptions);
        return new RunResult(repo.run(run.id()).orElseThrow(), repo.lines(run.id(), null));
    }

    public RunResult reconcileFromBank(String entityId, String currency, LocalDate date) {
        return reconcile(entityId, "BANK", currency, date, ledger.bankStatement(currency, date.toString()));
    }

    /** A bank credit is a debit to cash (1000) in the ledger; a bank debit is a credit to cash. */
    private String match(String entityId, String currency, LineInput in) {
        for (LedgerTxn t : ledger.byReference(entityId, in.externalRef())) {
            if (!"POSTED".equals(t.status())) {
                continue;
            }
            long cashNet = t.entries().stream()
                    .filter(e -> e.account().equals("1000") && e.currency().equals(currency))
                    .mapToLong(e -> "DEBIT".equals(e.direction()) ? e.amountMinor() : -e.amountMinor())
                    .sum();
            if (cashNet == in.amountMinor()) {
                return t.transactionId();
            }
        }
        return null;
    }

    public RunResult get(String runId, String status) {
        Run run = repo.run(runId).orElseThrow(() -> new NotFoundException("Reconciliation run", runId));
        return new RunResult(run, repo.lines(runId, status));
    }

    public Explanation explain(String lineId) {
        Line line = line(lineId);
        Run run = repo.run(line.runId()).orElseThrow();
        Explanation e = explainer.explain(line, ledger.byReference(run.entityId(), line.externalRef()));
        repo.updateLine(lineId, null, e.explanation(), null);
        return e;
    }

    /** Parks an unexplained line: bank credit -> Dr 1000 Cash / Cr 2900 Suspense; bank debit -> the reverse. */
    public Line toSuspense(String lineId) {
        Line line = line(lineId);
        requireStatus(line, "EXCEPTION");
        Run run = repo.run(line.runId()).orElseThrow();
        long amount = Math.abs(line.amountMinor());
        boolean credit = line.amountMinor() > 0;
        LedgerTxn txn = ledger.post(run.entityId(), "suspense:" + lineId, "SUSPENSE", "Unexplained bank line " + line.externalRef(),
                line.externalRef(), credit ? "1000" : "2900", credit ? "2900" : "1000", amount, line.currency());
        repo.updateLine(lineId, "SUSPENSE", null, txn.transactionId());
        return line(lineId);
    }

    /** Once explained, clears suspense against the right account, e.g. 1100 for a late network settlement. */
    public Line resolveSuspense(String lineId, String counterAccount) {
        Line line = line(lineId);
        requireStatus(line, "SUSPENSE");
        Run run = repo.run(line.runId()).orElseThrow();
        long amount = Math.abs(line.amountMinor());
        boolean credit = line.amountMinor() > 0;
        ledger.post(run.entityId(), "suspense-clear:" + lineId, "SUSPENSE_CLEARED", "Cleared suspense for " + line.externalRef(),
                line.externalRef(), credit ? "2900" : counterAccount, credit ? counterAccount : "2900", amount, line.currency());
        repo.updateLine(lineId, "RESOLVED", null, null);
        return line(lineId);
    }

    /** Customer and merchant money must be fully backed by cash plus what networks owe the platform. */
    public Safeguarding safeguarding(String entityId, String currency) {
        long cash = ledger.balance(entityId, "1000", currency).availableMinor();
        long receivable = ledger.balance(entityId, "1100", currency).availableMinor();
        long wallets = ledger.balance(entityId, "2010", currency).availableMinor();
        long merchants = ledger.balance(entityId, "2000", currency).availableMinor()
                + ledger.balance(entityId, "2100", currency).availableMinor();
        long surplus = cash + receivable - wallets - merchants;
        return new Safeguarding(entityId, currency, cash, receivable, wallets, merchants, surplus, surplus >= 0);
    }

    private Line line(String id) {
        return repo.line(id).orElseThrow(() -> new NotFoundException("Statement line", id));
    }

    private static void requireStatus(Line line, String status) {
        if (!status.equals(line.status())) {
            throw new BusinessRuleException("INVALID_LINE_STATE", "Line is " + line.status() + "; expected " + status);
        }
    }
}
