#!/usr/bin/env bash
# End-to-end smoke test through the gateway. Needs: curl, jq, and the platform running (docker compose up).
#   GATEWAY=http://localhost:8080 ./scripts/e2e-smoke.sh
# Every scenario is followed by the same check: the books must still balance.
set -euo pipefail
GW="${GATEWAY:-http://localhost:8080}"
TODAY=$(date +%F)

post() { # path json [idempotency-key]
  local key="${3:-$(uuidgen 2>/dev/null || cat /proc/sys/kernel/random/uuid)}"
  curl -sS -f -X POST "$GW$1" -H 'Content-Type: application/json' -H "Idempotency-Key: $key" -d "{\"payload\": $2}"
}
get() { curl -sS -f "$GW$1"; }
say() { printf '\n\033[1m== %s\033[0m\n' "$*"; }

assert_books() {
  local tb inv
  tb=$(get "/v1/entities/KE/reports/trial-balance")
  inv=$(get "/v1/entities/KE/invariants")
  [[ $(jq -r '.payload.balanced' <<<"$tb") == "true" ]] || { echo "TRIAL BALANCE NOT BALANCED"; jq . <<<"$tb"; exit 1; }
  [[ $(jq -r '.payload.trialBalanceBalanced' <<<"$inv") == "true" ]] || { echo "INVARIANTS FAILED"; jq . <<<"$inv"; exit 1; }
  echo "   books balanced ✔"
}

say "1. Onboard a Kenyan merchant with a 10% / 90-day rolling reserve"
MER=$(post /v1/merchants '{"entityId":"KE","name":"Duka Online","country":"KE","settlementCurrency":"KES","reservePercentBp":1000,"reserveDays":90}' | jq -r .payload.merchantId)
echo "   merchant $MER"

say "2. Card payment of 1,000.00 KES: authorize, then capture (fee + VAT go to the ledger)"
PI=$(post /v1/payment-intents "{\"merchantId\":\"$MER\",\"amountMinor\":100000,\"currency\":\"KES\",\"method\":\"CARD\"}" | jq -r .payload.id)
post "/v1/payment-intents/$PI/capture" '{}' | jq -c '.payload | {status, feeMinor, taxMinor, netMinor}'
assert_books

say "3. Two individuals with wallets; top-up; P2P transfer with excise duty"
C1=$(post /v1/individuals '{"entityId":"KE","name":"Amina","country":"KE","mainCurrency":"KES"}' | jq -r .payload.individualId)
C2=$(post /v1/individuals '{"entityId":"KE","name":"Brian","country":"KE","mainCurrency":"KES"}' | jq -r .payload.individualId)
W1=$(get "/v1/individuals/$C1/wallets" | jq -r '.payload[0].walletId')
W2=$(get "/v1/individuals/$C2/wallets" | jq -r '.payload[0].walletId')
S1=$(post "/v1/individuals/$C1/wallets" '{"currency":"KES","kind":"SAVINGS"}' | jq -r .payload.walletId)
U1=$(post "/v1/individuals/$C1/wallets" '{"currency":"USD","kind":"MAIN"}' | jq -r .payload.walletId)
post "/v1/wallets/$W1/top-ups" '{"amountMinor":2000000,"method":"MOBILE_MONEY"}' >/dev/null
post /v1/transfers "{\"fromWalletId\":\"$W1\",\"toWalletId\":\"$W2\",\"amountMinor\":50000}" | jq -c '.payload | {transferType, feeMinor, taxMinor}'
post /v1/transfers "{\"fromWalletId\":\"$W1\",\"toWalletId\":\"$S1\",\"amountMinor\":20000}" >/dev/null
assert_books

say "4. Wallet pays merchant; merchant pays an individual; KES -> USD conversion with FX spread"
post /v1/wallet-payments "{\"walletId\":\"$W1\",\"merchantId\":\"$MER\",\"amountMinor\":30000}" >/dev/null
post /v1/merchant-disbursements "{\"merchantId\":\"$MER\",\"walletId\":\"$W2\",\"amountMinor\":10000}" >/dev/null
post /v1/conversions "{\"fromWalletId\":\"$W1\",\"toWalletId\":\"$U1\",\"amountMinor\":130000}" | jq -c '.payload | {targetAmountMinor, targetCurrency}'
assert_books

say "5. Partial refund, then network settlement (network fee expensed)"
post "/v1/payment-intents/$PI/refunds" '{"amountMinor":20000,"reason":"damaged item"}' | jq -c '.payload | {status, refundedMinor}'
post /v1/settlements '{"entityId":"KE","currency":"KES"}' | jq -c '.payload | {batchId, grossMinor, feesMinor, netMinor}'
assert_books

say "6. Payouts: merchant to bank, individual withdrawal to mobile money (saga: hold -> bank -> post)"
post /v1/payouts "{\"beneficiaryType\":\"MERCHANT\",\"beneficiaryId\":\"$MER\",\"amountMinor\":20000,\"destinationType\":\"BANK\",\"destinationAccount\":\"0123456789\"}" | jq -c '.payload | {status, feeMinor}'
post /v1/payouts "{\"beneficiaryType\":\"WALLET\",\"beneficiaryId\":\"$W2\",\"amountMinor\":10000,\"destinationType\":\"MOBILE_MONEY\",\"destinationAccount\":\"254700000001\"}" | jq -c '.payload | {status, feeMinor, taxMinor}'
FAILED=$(post /v1/payouts "{\"beneficiaryType\":\"WALLET\",\"beneficiaryId\":\"$W2\",\"amountMinor\":5000,\"destinationType\":\"BANK\",\"destinationAccount\":\"999990000\"}" | jq -r .payload.status)
echo "   payout to a closed account: $FAILED (hold voided)"
assert_books

say "7. Personal loan: 12,000 KES, 24%/year, 12 months; disburse to wallet; first instalment"
LN=$(post /v1/loans "{\"productCode\":\"PL_12M\",\"borrowerType\":\"INDIVIDUAL\",\"borrowerId\":\"$C1\",\"walletId\":\"$W1\",\"principalMinor\":1200000}" | jq -r .payload.id)
post "/v1/loans/$LN/disburse" '{}' | jq -c '.payload | {status, outstandingMinor, allowanceMinor}'
get "/v1/loans/$LN/schedule" | jq -c '.payload[0]'
post "/v1/loans/$LN/repayments" '{"amountMinor":113472}' | jq -c '.payload | {interestPart, principalPart}'
assert_books

say "8. Merchant financing repaid automatically from the next card sale"
MF=$(post /v1/loans "{\"productCode\":\"MF_STANDARD\",\"borrowerType\":\"MERCHANT\",\"borrowerId\":\"$MER\",\"principalMinor\":500000}" | jq -r .payload.id)
post "/v1/loans/$MF/disburse" '{}' | jq -c '.payload | {outstandingMinor, unearnedFeeMinor}'
PI2=$(post /v1/payment-intents "{\"merchantId\":\"$MER\",\"amountMinor\":200000,\"currency\":\"KES\",\"method\":\"CARD\"}" | jq -r .payload.id)
post "/v1/payment-intents/$PI2/capture" '{}' >/dev/null
sleep 5   # Kafka event -> withholding
get "/v1/loans/$MF" | jq -c '.payload | {outstandingMinor, unearnedFeeMinor}'
assert_books

say "9. Time travel: empty the main wallet, run the lending day 70 days ahead -> missed instalment, late fee, recovery from savings"
AV=$(get "/v1/wallets/$W1" | jq -r .payload.availableMinor)
post /v1/transfers "{\"fromWalletId\":\"$W1\",\"toWalletId\":\"$W2\",\"amountMinor\":$((AV - 1150))}" >/dev/null   # leaves 0 after fee + excise
FUTURE=$(date -d "+70 days" +%F 2>/dev/null || date -v+70d +%F)
post /v1/lending-jobs/daily "{\"asOf\":\"$FUTURE\"}" | jq -c .payload
sleep 5   # LoanInstalmentMissed event -> collections recovers from the savings wallet
get "/v1/loans/$LN" | jq -c '.payload | {status, dpd, stage, interestDueMinor, feesDueMinor, allowanceMinor}'
get "/v1/recovery-cases?debtorId=$C1" | jq -c '.payload[] | {status, amountMinor, recoveredMinor}' || true
assert_books

say "10. Reconcile the bank statement; check safeguarding"
post /v1/reconciliation-runs/from-bank "{\"entityId\":\"KE\",\"currency\":\"KES\",\"date\":\"$TODAY\"}" | jq -c '.payload.run | {matched, exceptions}'
get "/v1/safeguarding?entityId=KE&currency=KES" | jq -c '.payload | {covered, surplusMinor}'

say "11. Financial statements"
get "/v1/entities/KE/reports/income-statement?currency=KES&from=$TODAY&to=$FUTURE" | jq -c '.payload | {totalRevenueMinor, totalExpensesMinor, profitMinor}'
get "/v1/entities/KE/reports/balance-sheet?currency=KES&asOf=$FUTURE" | jq -c '.payload | {totalAssetsMinor, totalLiabilitiesMinor, totalEquityMinor, balanced}'
assert_books
say "All scenarios passed"
