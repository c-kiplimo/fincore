# Runbook: trial balance not balanced

**Alert:** `GET /v1/entities/{entity}/invariants` returns `trialBalanceBalanced: false` or `projectionDrift` is not empty.

**What it means:** total debits no longer equal total credits for some currency, or the balance projection
(`account_balances`) disagrees with the entries. Both should be impossible; treat as a severity-1 incident.

**Check**
1. Stop money-moving traffic at the gateway if the drift is growing.
2. `GET /v1/entities/{entity}/reports/trial-balance` and note which currency is off.
3. Find the latest transactions for that currency: `GET /v1/transactions?entityId={entity}&limit=100`.
4. Look for manual database changes (the append-only trigger blocks entry edits; check whether it was dropped).

**Fix**
- Projection drift only: rebuild `account_balances` from `ledger_entries` (sum of debit-positive amounts per account)
  in a maintenance window, then re-run the invariant check.
- Unbalanced entries: never edit them. Post a correcting transaction (or a reversal plus the correct entry) and
  record an incident with the root cause.
