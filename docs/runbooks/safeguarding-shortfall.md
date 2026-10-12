# Runbook: safeguarding shortfall

**Alert:** `GET /v1/safeguarding?entityId=..&currency=..` returns `covered: false`.

**What it means:** cash (1000) plus what networks owe (1100) is less than what the platform owes wallets (2010) and
merchants (2000 + 2100). Customer money is not fully backed.

**Check**
1. Run bank reconciliation for the day; look for unposted settlements or unknown debits.
2. Check for large negative merchant balances or wallets (collections may be needed).
3. Check whether a settlement batch failed to post.

**Fix**
- Post missing settlements or clear suspense lines.
- Open recovery cases for negative balances.
- If a real shortfall remains, the business must top up the safeguarding account; record the incident.
