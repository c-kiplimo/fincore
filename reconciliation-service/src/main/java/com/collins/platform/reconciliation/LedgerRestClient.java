package com.collins.platform.reconciliation;

import com.collins.platform.commons.http.ServiceClient;
import com.collins.platform.commons.http.ServiceClientFactory;
import com.collins.platform.reconciliation.ReconModels.LedgerBalance;
import com.collins.platform.reconciliation.ReconModels.LedgerTxn;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Reconciliation reads the ledger over REST (DDD: conformist to the ledger's model). It only writes suspense
 * entries, through the same balanced-transaction API every service uses.
 */
@Component
public class LedgerRestClient {

    private final ServiceClient ledger;
    private final ServiceClient simulator;

    public LedgerRestClient(ServiceClientFactory factory, @Value("${platform.services.ledger-url}") String ledgerUrl,
                            @Value("${platform.services.simulator-url}") String simulatorUrl) {
        this.ledger = factory.create("ledger-service", ledgerUrl);
        this.simulator = factory.create("bank", simulatorUrl);
    }

    public List<LedgerTxn> byReference(String entityId, String referenceId) {
        return ledger.getList("/v1/transactions?entityId=" + entityId + "&referenceId=" + referenceId, LedgerTxn.class);
    }

    public LedgerBalance balance(String entityId, String account, String currency) {
        return ledger.get("/v1/entities/" + entityId + "/accounts/" + account + "/balance?currency=" + currency, LedgerBalance.class);
    }

    public LedgerTxn post(String entityId, String key, String type, String description, String referenceId,
                          String debitAccount, String creditAccount, long amountMinor, String currency) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("idempotencyKey", key);
        payload.put("entityId", entityId);
        payload.put("transactionType", type);
        payload.put("description", description);
        payload.put("referenceType", "statement_line");
        payload.put("referenceId", referenceId);
        payload.put("entries", List.of(
                Map.of("account", debitAccount, "direction", "DEBIT", "amountMinor", amountMinor, "currency", currency),
                Map.of("account", creditAccount, "direction", "CREDIT", "amountMinor", amountMinor, "currency", currency)));
        return ledger.post("/v1/transactions", payload, LedgerTxn.class);
    }

    public List<ReconModels.LineInput> bankStatement(String currency, String date) {
        return simulator.getList("/sim/bank/statements?currency=" + currency + "&date=" + date, ReconModels.BankLine.class).stream()
                .map(l -> new ReconModels.LineInput(l.bankRef(), l.amountMinor(), l.valueDate(), l.description()))
                .toList();
    }
}
