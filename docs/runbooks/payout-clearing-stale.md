# Runbook: payout clearing older than 24 hours

**Alert:** account `2300 Payout clearing` has a balance from transactions older than 24 hours, or payouts stay `SENT`.

**What it means:** money left the merchant or wallet balance (pending hold) but the bank never confirmed or rejected it.

**Check**
1. `GET /v1/payouts?beneficiaryId=...` and find payouts in `SENDING` or `SENT` with high `attempts`.
2. Ask the bank (simulator: `GET /sim/bank/payouts/{payoutId}`).
3. Compare with today's bank statement through reconciliation.

**Fix**
- Bank says PAID: `POST /v1/payouts/{id}/retry` completes the saga (commit hold, Dr 2300 / Cr 1000).
- Bank says FAILED or never received it: retry also voids the hold, returning the money to the source balance.
- Never post directly to 2300 by hand.
