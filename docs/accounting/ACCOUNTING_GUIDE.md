# The accounting behind this platform

A guide for engineers who have never studied accounting. Every idea here is used somewhere in the code, and every
section says where. Read it top to bottom once; after that, use it as a reference when you work on a service.

> Amounts in the examples are written as normal numbers (1,000.00 KES). In the code and the APIs they are **minor
> units** (`100000` = 1,000.00 KES). Tax rates are examples or 2026 published figures; this is learning material, not
> tax, legal or accounting advice.

---

## Contents

1. [How this guide came about](#1-how-this-guide-came-about)
2. [The seven foundations](#2-the-seven-foundations)
3. [The accounting principles behind the design](#3-the-accounting-principles-behind-the-design)
4. [The chart of accounts](#4-the-chart-of-accounts)
5. [Card payments: capture, settlement, refunds, chargebacks](#5-card-payments)
6. [Merchant balances: reserves, payouts, negative balances](#6-merchant-balances)
7. [Wallets: top-ups, transfers, paying merchants, withdrawals](#7-wallets)
8. [Fees and taxes on every transaction](#8-fees-and-taxes)
9. [Foreign currency: conversion, spread, revaluation](#9-foreign-currency)
10. [Lending part 1: merchant financing](#10-lending-part-1-merchant-financing)
11. [Lending part 2: instalment loans](#11-lending-part-2-instalment-loans)
12. [Collections: recovering debts across accounts](#12-collections)
13. [Reconciliation, suspense and safeguarding](#13-reconciliation-suspense-and-safeguarding)
14. [Period close and the financial statements](#14-period-close-and-financial-statements)
15. [Multinational: entities, intercompany, consolidation](#15-multinational)
16. [How the code enforces all this](#16-how-the-code-enforces-all-this)
17. [Glossary](#17-glossary)
18. [Sources and further reading](#18-sources-and-further-reading)

---

## 1. How this guide came about

This guide was written in three steps.

1. **Start from the standard body of accounting knowledge.** Double-entry bookkeeping has been the rule since Luca
   Pacioli described it in 1494. The rest follows the international standards most countries use (IFRS) and their US
   counterparts (US GAAP): revenue recognition (IFRS 15), financial instruments and expected credit losses (IFRS 9 /
   US ASC 326 "CECL"), foreign currency (IAS 21 / ASC 830) and consolidation (IFRS 10). These are linked in
   [section 18](#18-sources-and-further-reading).
2. **Translate it for engineers.** Engineer-oriented explanations, especially Martin Kleppmann's *Accounting for
   Computer Scientists* and Modern Treasury's *Accounting for Developers*, show how ledgers map to software. Payment
   patterns (idempotency keys, API versioning) follow Stripe's published engineering posts.
3. **Build it and check it.** Every journal entry here is posted by the code, and the arithmetic (fees, taxes,
   interest, amortization, staging) is covered by unit tests. While building, the loan example was recomputed exactly.
   The instalment is **1,134.72**, not the 1,134.71 that rounding by hand suggests. That is why the tests use exact
   numbers.

Where a rule is a simplification (for example stopping interest on bad loans), the guide says so.

---

## 2. The seven foundations

### 2.1 Accounts are buckets

An **account** is a named bucket holding an amount. Every account has one of five types:

| Type | Plain meaning | Example in this platform |
|---|---|---|
| Asset | What the business owns or is owed | Cash at bank; money the card network owes us |
| Liability | What the business owes others | Money we hold for merchants and wallet users |
| Equity | What belongs to the owners | Capital put in; profits kept |
| Revenue | What the business earns | Payment fees, interest, FX spread |
| Expense | What the business spends or loses | Card-network fees, credit losses |

There is one more kind, the **contra account**: an account that sits next to another and reduces it. *Allowance for
credit losses* reduces *Loan receivable*. In the code its type is `CONTRA_ASSET`.

### 2.2 The accounting equation

At every moment:

```
Assets = Liabilities + Equity
```

Revenue and expenses are the parts of equity that change during a period, so the working version is:

```
Assets + Expenses = Liabilities + Equity + Revenue
```

### 2.3 Debits and credits are just left and right

**Debit = left side** of the equation, **credit = right side**. An account on the left grows with a debit; an account on
the right grows with a credit.

| Account type | Increases with | Decreases with | Normal balance |
|---|---|---|---|
| Asset, Expense | Debit | Credit | Debit |
| Liability, Equity, Revenue, Contra-asset | Credit | Debit | Credit |

*Code:* `normal_side` in `ledger-service/.../db/migration/V2__seed_entities_and_chart.sql`;
`JournalRules.natural()` turns a debit-positive balance into the "natural" balance shown in the API.

### 2.4 Double entry

Every transaction has **two or more entries**, and **total debits = total credits** in each currency. Money never
appears or disappears; it moves between buckets.

*Code:* `JournalRules.validate()` rejects a transaction that does not balance with `TRANSACTION_NOT_BALANCED`.

### 2.5 Journal, ledger, trial balance

1. **Journal:** transactions in time order (`ledger_transactions` + `ledger_entries`).
2. **General ledger:** the same entries grouped by account (`GET /v1/entities/{e}/accounts/{account}/statement`).
3. **Trial balance:** every account's balance; total debit balances must equal total credit balances
   (`GET /v1/entities/{e}/reports/trial-balance`).
4. **Financial statements:** the trial balance arranged into the balance sheet and income statement ([section 14](#14-period-close-and-financial-statements)).

### 2.6 Accrual, not cash

Revenue is recorded when it is **earned** and expenses when they are **incurred**, not when cash moves. When a
payment is captured the fee is earned, so it is recorded then, although the network pays days later. Loan interest is
recorded every day (accrued), although the borrower pays monthly.

### 2.7 Never edit, always reverse

Posted entries are never changed or deleted. A mistake is fixed by posting a **reversal** (the same entries, flipped)
and then the correct entry.

*Code:* a database trigger (`ledger_entries_append_only` in `V1__ledger_schema.sql`) blocks every UPDATE and DELETE on
entries; `POST /v1/transactions/{id}/reverse` posts the reversal.

---

## 3. The accounting principles behind the design

Accounting rests on a handful of principles. Each one became a design decision.

| Principle | What it says | Where it shows up in the platform |
|---|---|---|
| **Duality** (double entry) | Every event has two equal sides | `JournalRules.validate()`; every service posts through `ledger.v1` |
| **Entity concept** | A business's books are separate from its owners' and from other companies' | One set of books per legal entity (`entity_id`); intercompany accounts between them |
| **Monetary unit** | Record in money, in one measuring currency per entity | Integer minor units (`Money`, `CurrencyUnits`); each entity has a functional currency |
| **Accrual basis** | Record when earned or incurred | Fees at capture; daily interest accrual; accrued expenses at month-end |
| **Revenue recognition** (IFRS 15) | Recognise revenue as you deliver the service | Payment fee at capture; merchant-financing fee spread over repayments |
| **Matching** | Put costs in the same period as the revenue they helped earn | Network fees expensed at settlement in the same period as the captures |
| **Prudence** | Recognise expected losses early; don't overstate assets | Loan-loss allowance from day one (IFRS 9 stage 1) |
| **Substance over form** | Record what really happened, not what it is called | A wallet balance is a *liability* (we owe it), not "the customer's money in our bank" |
| **Periodicity** | Report in fixed periods | Accounting periods, month-end close, closed periods are locked |
| **Consistency** | Use the same method every time | Rules as data with versions (tax rules, fee plans, day-count) |
| **Measurement / remeasurement** | Some balances are re-measured at today's value | Month-end FX revaluation of foreign-currency balances (IAS 21) |
| **Materiality and rounding** | Small differences must still be explained | Half-even rounding, stored per transaction; last instalment absorbs rounding |
| **Full disclosure** | Users need the information behind the numbers | Quotes show every fee and tax; loan schedules show total cost of credit |
| **Audit trail** | Anyone can trace a number to its source | Append-only entries, idempotency keys, references, request ids |

---

## 4. The chart of accounts

Every legal entity (`KE`, `UK`, `US`) gets the same chart, seeded in
`ledger-service/src/main/resources/db/migration/V2__seed_entities_and_chart.sql`. Accounts can have **sub-ledgers**:
`2000:mer_abc` is "Merchant payable" for one merchant, `2010:wal_xyz` one wallet, `2510:KE` VAT owed in Kenya.
Sub-ledger accounts are created the first time something is posted to them.

| Code | Account | Type | Normal | Plain meaning |
|---|---|---|---|---|
| 1000 | Cash at bank | Asset | Debit | Real money in the platform's bank account |
| 1100 | Settlement receivable | Asset | Debit | Owed to us by the card network or mobile-money provider |
| 1200 | Merchant advance receivable | Asset | Debit | Merchant financing still owed (principal + fee) |
| 1210 | Unearned financing fee | Contra-asset | Credit | Financing fee not yet earned |
| 1220 | Allowance for credit losses | Contra-asset | Credit | Losses we expect on loans |
| 1230 | Loan receivable | Asset | Debit | Principal owed on instalment loans |
| 1240 | Interest receivable | Asset | Debit | Interest earned but not yet paid |
| 1250 | Loan fees receivable | Asset | Debit | Late fees not yet paid |
| 1300 | Merchant receivable | Asset | Debit | A merchant owes us (negative balance) |
| 1310 | Customer receivable | Asset | Debit | An individual owes us (negative wallet) |
| 1400 | FX clearing | Asset | Debit | Joins two currencies in a conversion; should net to zero |
| 1410 | FX revaluation adjustment | Asset | Debit | Change in value of foreign-currency balances |
| 1600 | Intercompany receivable | Asset | Debit | Another group company owes this one |
| 2000 | Merchant payable | Liability | Credit | Money we hold for a merchant |
| 2010 | Customer wallet | Liability | Credit | Money we hold for an individual |
| 2100 | Merchant reserve | Liability | Credit | Merchant money held back for risk |
| 2200 | Disputed funds held | Liability | Credit | Merchant money frozen during a chargeback |
| 2300 | Payout clearing | Liability | Credit | Payouts sent but not yet confirmed by the bank |
| 2400 | Accrued expenses | Liability | Credit | Costs incurred, not yet billed |
| 2500 | Excise duty payable | Liability | Credit | Excise collected, owed to the tax authority |
| 2510 | VAT / GST payable | Liability | Credit | VAT/GST collected, owed to the tax authority |
| 2520 | Withholding tax payable | Liability | Credit | Tax withheld from cross-border payments |
| 2600 | Intercompany payable | Liability | Credit | This company owes another group company |
| 2900 | Suspense | Liability | Credit | Money received that we cannot explain yet |
| 3000 | Owner's equity | Equity | Credit | Capital the owners put in |
| 3100 | Retained earnings | Equity | Credit | Profits kept from closed periods |
| 3200 | FX translation reserve | Equity | Credit | Group-level difference from translating currencies |
| 4000 | Payment fee revenue | Revenue | Credit | Our fee on payments |
| 4100 | Financing fee revenue | Revenue | Credit | Merchant-financing fees earned |
| 4110 | Interest income | Revenue | Credit | Interest earned on loans |
| 4120 | Late fee income | Revenue | Credit | Late fees charged |
| 4200 | Dispute fee revenue | Revenue | Credit | Fee charged per chargeback |
| 4300 | Transfer and withdrawal fee revenue | Revenue | Credit | P2P and withdrawal fees |
| 4500 | FX spread revenue | Revenue | Credit | Margin on currency conversion |
| 4600 | Intercompany service revenue | Revenue | Credit | Fees charged to another group company |
| 4900 | FX gain | Revenue | Credit | Gains from rate changes and rounding |
| 5000 | Network fees expense | Expense | Debit | What the card network charges us |
| 5100 | Credit loss expense | Expense | Debit | Expected and actual loan losses |
| 5200 | FX loss | Expense | Debit | Losses from rate changes |
| 5300 | Chargeback loss | Expense | Debit | Debts we absorb (merchant or wallet) |
| 5400 | Bank charges | Expense | Debit | Bank fees |
| 5600 | Intercompany service expense | Expense | Debit | Fees paid to another group company |

---

## 5. Card payments

*Service:* `payments-service` (`PaymentService.java`). Example: merchant **M1** in Kenya, a customer pays
**1,000.00 KES**; fee 2.9% = **29.00**; VAT 16% on the fee = **4.64** (Kenya, Finance Act 2026).

### 5.1 Authorize: no entry

The network checks the card and reserves the amount. No money has moved, so **nothing is posted**. The intent becomes
`AUTHORIZED`.

### 5.2 Capture: revenue, liability, receivable

The merchant confirms the sale. Now the network owes us, we owe the merchant, we have earned a fee, and we owe VAT.

| Account | Debit | Credit |
|---|---|---|
| 1100 Settlement receivable | 1,000.00 | |
| 2000 Merchant payable:M1 | | 966.36 |
| 4000 Payment fee revenue | | 29.00 |
| 2510 VAT payable:KE | | 4.64 |

The fee and VAT come from a **quote** (see [section 8](#8-fees-and-taxes)). The event `PaymentCaptured` is published
through the outbox.

### 5.3 Settlement: receivable becomes cash, minus the network's cost

Days later the network pays everything it owes in one batch, keeping its own fee (1.5% in the simulator = 15.00).

| Account | Debit | Credit |
|---|---|---|
| 1000 Cash at bank | 985.00 | |
| 5000 Network fees expense | 15.00 | |
| 1100 Settlement receivable | | 1,000.00 |

Our profit on this payment: 29.00 fee − 15.00 network cost = **14.00**. (The 4.64 VAT is not ours; it goes to the tax
authority.) The batch id is the reference reconciliation uses to match the bank statement.

### 5.4 Refund: a liability goes down

The customer gets 200.00 back; the network collects it at the next settlement. The platform keeps its fee (as many
processors do).

| Account | Debit | Credit |
|---|---|---|
| 2000 Merchant payable:M1 | 200.00 | |
| 1100 Settlement receivable | | 200.00 |

### 5.5 Chargeback: held funds and a fee

The customer's bank disputes the payment and the network pulls the money. We charge the merchant a dispute fee of 1,500.00.

| Step | Debit | Credit |
|---|---|---|
| Dispute opened | 2000 Merchant payable:M1 (amount + fee) | 2200 Disputed funds held (amount) · 4200 Dispute fee revenue (fee) |
| Network pulls the money | 2200 Disputed funds held | 1100 Settlement receivable |
| Won (money comes back) | 1100 Settlement receivable | 2000 Merchant payable:M1 |
| Lost | nothing more | |

**What you learn:** a liability can be moved to a "held" liability while an outcome is uncertain.

---

## 6. Merchant balances

### 6.1 Rolling reserve (`accounts-service`, `MerchantService.onPaymentCaptured`)

New or risky merchants have part of every payment held back. M1 has a 10% / 90-day reserve: 10% of 966.36 = 96.64.

| Account | Debit | Credit |
|---|---|---|
| 2000 Merchant payable:M1 | 96.64 | |
| 2100 Merchant reserve:M1 | | 96.64 |

We still owe the merchant the same total; part of it simply can't be paid out. After 90 days the release reverses it.
This runs from the `PaymentCaptured` Kafka event, so it shows **event-driven accounting** and **idempotent consumers**.

### 6.2 Payouts: pending, posted, voided (`payouts-service`, `PayoutSaga`)

A payout is a **saga**: each step has a compensating step.

| Step | Ledger | Debit | Credit |
|---|---|---|---|
| 1. Hold (pending) | pending transaction | 2000 Merchant payable:M1 (amount + fee + tax) | 2300 Payout clearing · 4300 fee · 25xx tax |
| 2a. Bank pays | commit the hold, then post | 2300 Payout clearing | 1000 Cash at bank |
| 2b. Bank rejects | void the hold (compensation) | – | – |
| 2c. Bank times out | leave it pending; retry job asks the bank later | – | – |

**Pending vs posted:** a pending transaction already reduces what can be spent (`availableMinor`), but is not in the
financial statements until it is posted. A voided one disappears from balances but stays in history.

### 6.3 Negative merchant balance (`collections`, `fromNegativeMerchant`)

Refunds and chargebacks can push a merchant below zero; then the merchant owes us. A negative liability is really an
asset, so we reclassify it:

| Account | Debit | Credit |
|---|---|---|
| 1300 Merchant receivable:M1 | 500.00 | |
| 2000 Merchant payable:M1 | | 500.00 |

---

## 7. Wallets

*Service:* `accounts-service` (`WalletService`). **Key lesson:** wallet money is a **liability** (2010). Moving money
between wallets moves **no cash**; it only changes whom we owe. Cash moves only on top-ups and withdrawals.

| Operation | Debit | Credit | Notes |
|---|---|---|---|
| Top-up 2,000.00 by card or mobile money | 1100 Settlement receivable 2,000 | 2010 Wallet:C1 2,000 | Becomes cash at settlement |
| Move 500 to own savings | 2010 Wallet:C1-MAIN 500 | 2010 Wallet:C1-SAVINGS 500 | No fee, no tax |
| P2P 500 to C2, fee 10, excise 15% = 1.50 | 2010 Wallet:C1 511.50 | 2010 Wallet:C2 500 · 4300 fee 10 · 2500 Excise:KE 1.50 | Sender pays fee + tax |
| C1 pays merchant 300 (fee 1.5% = 4.50, VAT 0.72) | 2010 Wallet:C1 300 | 2000 Merchant:M1 294.78 · 4000 fee 4.50 · 2510 VAT:KE 0.72 | Merchant pays fee + tax |
| Merchant pays C2 700 | 2000 Merchant:M1 700 | 2010 Wallet:C2 700 | Seller earnings, prizes, refunds to wallet |
| C2 withdraws 1,000 (fee 30, excise 4.50) | see payouts saga | | Hold, then bank confirms |

Wallet debits use `enforce_non_negative`, so the ledger refuses to let a wallet go below zero from a transfer
(`INSUFFICIENT_FUNDS`). A wallet can still go negative through a chargeback on a top-up; collections handles that.

**Safeguarding:** wallet providers must keep customer money fully backed. `GET /v1/safeguarding` checks
cash (1000) + settlement receivable (1100) ≥ customer wallets (2010) + merchant balances (2000 + 2100).

---

## 8. Fees and taxes

*Service:* `pricing-service` (`QuoteService`, tax rules in `V2__seed_pricing.sql`).

Every money movement is priced by a **quote** first: principal, fees, taxes, FX and total. The service then sends the
`quote_id`, which can be used **once** before it **expires**.

**Taxes collected are liabilities, never revenue.** We collect VAT or excise on behalf of the government and owe it
until we pay it over.

| Tax type | Charged on | Account | Example rule |
|---|---|---|---|
| VAT / GST | Our fee (the service) | 2510:{country} | Kenya 16% on payment processing (from 2026-07-01) |
| Excise duty | Our fee | 2500:{country} | Kenya 15% on money-transfer fees |
| Withholding tax | A payment we make abroad | 2520:{country} | 10% held back from a foreign partner's fee |
| Reverse charge | Nothing collected | – | Cross-border B2B: the customer accounts for the VAT |

**Paying the tax authority:** debit the payable (2500/2510/2520), credit 1000 Cash.

**Withholding example** (we pay a foreign partner 1,000 and must withhold 10%):

| Account | Debit | Credit |
|---|---|---|
| 5000 Network fees expense | 1,000 | |
| 1000 Cash at bank | | 900 |
| 2520 Withholding tax payable | | 100 |

**Rules as data:** each tax rule has a jurisdiction, product, rate, account and validity dates. A Finance Act change
is a new **version** (`POST /v1/tax-rules`); the old version is closed the day before, so an old transaction can always
be recalculated with the rule of its date.

---

## 9. Foreign currency

*Services:* `accounts-service` (`WalletService.convert`), `pricing-service` (FX quotes), `ledger-service` (`RevaluationService`).

### 9.1 Each transaction balances per currency

You cannot add shillings to dollars. A conversion is joined through **1400 FX clearing**, which appears in both currencies.

C1 converts **USD 100** to KES. Mid-market rate 130; with a 1% spread the customer gets 128.70.

| Account | Currency | Debit | Credit |
|---|---|---|---|
| 2010 Wallet:C1-USD | USD | 100.00 | |
| 1400 FX clearing | USD | | 100.00 |
| 1400 FX clearing | KES | 13,000.00 | |
| 2010 Wallet:C1-KES | KES | | 12,870.00 |
| 4500 FX spread revenue | KES | | 130.00 |

### 9.2 Realized gain or loss

When the platform later sells the USD at the bank for 12,800 KES instead of 13,000, FX clearing (KES) is left with
200 KES that will never come in: a **realized FX loss** (debit 5200, credit 1400).

### 9.3 Revaluation (unrealized)

At month-end, foreign-currency balances still held are re-measured at the closing rate. The amount in USD does not
change; its value in the entity's functional currency does. `POST /v1/entities/{e}/revaluations` compares each
account with the last rate used:

- An **asset** worth more, or a **liability** worth less → gain: debit 1410, credit 4900.
- The opposite → loss: debit 5200, credit 1410.

The first run only stores the baseline rate.

### 9.4 Minor units and rounding

Never assume two decimals: JPY has 0, KES 2, KWD 3 (`CurrencyUnits`). Rounding is half-even ("banker's rounding"),
which does not drift upwards over millions of transactions.

---

## 10. Lending part 1: merchant financing

*Service:* `lending-service` (`LoanService`), product `MF_STANDARD`. This is how Stripe Capital-style financing works:
the merchant gets money now and repays from future card sales.

M1 gets **10,000 KES** for a fixed fee of **1,000 KES** (10%), repaid by withholding **10%** of every sale.

**Disburse** (money goes to the merchant's balance; they can pay it out):

| Account | Debit | Credit |
|---|---|---|
| 1200 Advance receivable:L1 | 11,000 | |
| 2000 Merchant payable:M1 | | 10,000 |
| 1210 Unearned financing fee:L1 | | 1,000 |

The fee is **not revenue yet**: revenue is recognised as the service is delivered (IFRS 15 idea).

**Withholding** (from the `PaymentCaptured` event; 10% of a 966.36 net payment = 96.64):

| Account | Debit | Credit |
|---|---|---|
| 2000 Merchant payable:M1 | 96.64 | |
| 1200 Advance receivable:L1 | | 96.64 |
| 1210 Unearned financing fee:L1 | 8.79 | |
| 4100 Financing fee revenue | | 8.79 |

Fee earned = 1,000 × 96.64 / 11,000 = 8.79. The last repayment takes whatever unearned fee is left, so the total is exact.

**Write-off** of a financing advance cancels the unearned fee (it was never income) and uses the allowance:

| Account | Debit | Credit |
|---|---|---|
| 1210 Unearned financing fee:L1 | unearned part | |
| 1220 Allowance:L1 | allowance | |
| 5100 Credit loss expense | the rest | |
| 1200 Advance receivable:L1 | | everything still owed |

---

## 11. Lending part 2: instalment loans

*Service:* `lending-service`, product `PL_12M`. C1 borrows **12,000 KES** for **12 months** at **24% a year** (2% a month),
reducing balance.

### 11.1 The schedule (amortization)

Equal monthly instalments from the annuity formula:

```
instalment = P × r / (1 − (1 + r)^−n) = 12,000 × 0.02 / (1 − 1.02^−12) = 1,134.7151… → 1,134.72
```

| Month | Opening principal | Interest (2%) | Principal | Closing principal |
|---|---|---|---|---|
| 1 | 12,000.00 | 240.00 | 894.72 | 11,105.28 |
| 2 | 11,105.28 | 222.11 | 912.61 | 10,192.67 |
| 3 | 10,192.67 | 203.85 | 930.87 | 9,261.80 |
| … | | | | |
| 12 | 1,112.42 | 22.25 | 1,112.42 | 0.00 |

The final instalment (1,134.67) absorbs the rounding. *Code:* `InterestMath.annuity`; checked in `PricingRulesTest`.

### 11.2 Disbursement: no cash leaves yet

| Account | Debit | Credit |
|---|---|---|
| 1230 Loan receivable:L1 | 12,000 | |
| 2010 Wallet:C1 | | 12,000 |

### 11.3 Daily interest accrual

A scheduled job (`LoanJobs`) posts interest as it is earned, using the jurisdiction's **day-count convention**:

```
interest = principal × annual rate × days / basis      (ACT/365, ACT/360 or 30/360)
```

10,000 at 12% for 30 days is 98.63 under ACT/365 but 100.00 under ACT/360.

| Account | Debit | Credit |
|---|---|---|
| 1240 Interest receivable:L1 | daily interest | |
| 4110 Interest income | | daily interest |

### 11.4 Repayment and the waterfall

Every repayment pays **fees first, then interest, then principal**:

| Account | Debit | Credit |
|---|---|---|
| 2010 Wallet:C1 | 1,134.72 | |
| 1250 Loan fees receivable:L1 | | fees part |
| 1240 Interest receivable:L1 | | interest part |
| 1230 Loan receivable:L1 | | principal part |

### 11.5 Delinquency, late fees, days past due

If an instalment is not paid on its due date, the loan is **delinquent** and **days past due (DPD)** start counting.
After the 5-day grace period a 200 KES late fee is charged (debit 1250, credit 4120), and `LoanInstalmentMissed` is
published so collections can act.

### 11.6 Expected credit losses (IFRS 9 stages)

The **allowance for credit losses** (1220) holds the losses we expect. It is built from day one (prudence) and grows as risk grows.

| Stage | Trigger | Allowance covers | Rate used here (illustrative) |
|---|---|---|---|
| 1 Performing | < 30 DPD | 12-month expected losses | 1% |
| 2 Significant increase in risk | 30+ DPD | Lifetime expected losses | 10% |
| 3 Credit-impaired | 90+ DPD | Lifetime losses, defaulted | 50% |

Moving 9,000 owed from stage 1 to 2 raises the allowance from 90 to 900: debit 5100 Credit loss expense 810,
credit 1220 Allowance 810.

**Non-accrual** (simplification): at stage 3 the platform stops accruing interest. Strictly, IFRS 9 keeps recognising
interest on the net carrying amount; stopping is a common practical simplification.

### 11.7 Write-off, early repayment, restructuring

- **Write-off:** the allowance absorbs what it covers, the rest is credit loss expense, and principal, interest and
  fees receivable all go to zero.
- **Early repayment:** accrue interest to today, then repay everything; no future interest is charged.
- **Restructuring:** unpaid future instalments are replaced by a longer schedule. Changing a schedule posts nothing in
  the ledger.
- **Closing a loan:** when fully repaid, any allowance left is released (debit 1220, credit 5100).

---

## 12. Collections

*Service:* `lending-service`, collections module (`CollectionsService`). When anyone owes the platform, a
**recovery case** collects it from **all** their accounts in a fixed order (this is called **set-off**; in real life
it must be allowed by the customer's terms and local law).

1. Same currency: savings first, then main.
2. Other currencies, converted at the mid rate (no spread on recoveries).
3. Money arriving later (auto-sweep).
4. Write-off.

**Negative wallet example.** C1's card top-up of 2,000 is charged back and the main wallet ends at −1,310.

| Step | Debit | Credit |
|---|---|---|
| Reclassify the debt | 1310 Customer receivable:C1 1,310 | 2010 Wallet:C1-MAIN 1,310 |
| Recover from savings (500) | 2010 Wallet:C1-SAVINGS 500 | 1310 Customer receivable:C1 500 |
| Recover from USD wallet (USD 3 at 130 = 390 KES) | 2010 Wallet:C1-USD USD 3 · 1400 FX clearing KES 390 | 1400 FX clearing USD 3 · 1310 Customer receivable KES 390 |
| Write off the rest (420) | 5300 Chargeback loss 420 | 1310 Customer receivable:C1 420 |
| Money arrives later (sweep 420) | 2010 Wallet:C1-MAIN 420 | 5300 Chargeback loss 420 |

**A recovery after write-off reduces the loss** rather than counting as new revenue (credit 5300, or 5100 for loans).

For overdue loans, each recovery step is a loan repayment, so the waterfall still applies.

---

## 13. Reconciliation, suspense and safeguarding

*Service:* `reconciliation-service`.

**Reconciliation** proves that your records agree with the outside world. Each bank-statement line must be explained
by a **posted** ledger transaction with the **same reference** that moves **the same amount through cash (1000)**:

- A settlement credit → matches `NETWORK_SETTLEMENT` (reference = batch id, debit 1000).
- A payout debit → matches `PAYOUT_CONFIRMED` (reference = payout id, credit 1000).

Lines that don't match are **exceptions**. A person investigates (optionally with the AI explainer) and can park the
money in **suspense**:

| Bank line | Debit | Credit |
|---|---|---|
| Unknown deposit of 500 | 1000 Cash at bank 500 | 2900 Suspense 500 |
| Later found to be a late network settlement | 2900 Suspense 500 | 1100 Settlement receivable 500 |

A suspense balance that stays non-zero is a warning sign. The AI explainer only reads and suggests; it never posts.

---

## 14. Period close and financial statements

*Service:* `ledger-service` (`PeriodService`, `ReportService`).

### 14.1 Month-end close

1. **Accrue costs not yet billed:** September bank charges billed in October are recorded in September
   (debit 5400 Bank charges, credit 2400 Accrued expenses), then cleared when paid.
2. **Revalue foreign-currency balances** ([section 9.3](#93-revaluation-unrealized)).
3. **Run the trial balance:** it must balance in every currency.
4. **Closing entries:** every revenue and expense balance is moved into **3100 Retained earnings**, so next month's
   income statement starts at zero.
5. **Lock the period:** postings into it are refused (`PERIOD_CLOSED`); corrections go into the next open period.

`POST /v1/entities/{e}/periods/{yyyy-MM}/close` does steps 3 to 5.

### 14.2 The three reports

| Report | Question it answers | Built from | Endpoint |
|---|---|---|---|
| Trial balance | Do the books balance? | Every account's balance | `/reports/trial-balance` |
| Income statement (P&L) | Did we make money this period? | Revenue − expenses for the dates | `/reports/income-statement` |
| Balance sheet | What do we own and owe right now? | Assets = liabilities + equity (+ profit not yet closed) | `/reports/balance-sheet` |

Contra-assets (1210, 1220) are shown as reductions of assets. Before a period is closed, its profit appears in the
balance sheet as "current period profit"; after closing it sits in retained earnings.

---

## 15. Multinational

*Service:* `ledger-service` (`ConsolidationService`, `RevaluationService`).

- **One legal entity per country**, each with its own books and **functional currency** (KE → KES, UK → GBP, US → USD).
- A transaction always balances **inside one entity**. Money between entities is two transactions (one in each
  entity's books) through **intercompany** accounts.

**Intercompany:** KE charges UK a 1,000 USD service fee.

| Books | Debit | Credit |
|---|---|---|
| Platform KE | 1600 Intercompany receivable:UK | 4600 Intercompany service revenue |
| Platform UK | 5600 Intercompany service expense | 2600 Intercompany payable:KE |

**Consolidation** (`POST /v1/consolidation`):

1. Close each entity.
2. **Translate** into the reporting currency: assets and liabilities at the closing rate; revenue and expenses at the
   average rate (IAS 21). The difference goes to **3200 FX translation reserve**.
3. **Eliminate** intercompany balances (1600, 2600, 4600, 5600); otherwise the group would count money it owes itself.
4. Add up the result into group statements. The report shows whether intercompany balances matched.

---

## 16. How the code enforces all this

| Rule | Enforced by |
|---|---|
| Debits = credits per currency | `JournalRules.validate` (and tests with random transactions) |
| Entries never change | `ledger_entries_append_only` database trigger |
| No double posting on retries | Idempotency keys: `ledger_transactions (entity_id, idempotency_key)` unique; `IdempotencyService` on every POST |
| No overdraft | `enforce_non_negative` lines + row locks in a fixed order (`LedgerRepository.lockBalances`) |
| No posting into a closed month | `PostingService` checks `accounting_periods` |
| Balances can always be rebuilt | Balances are a projection; `InvariantService` compares them with the entries |
| Money is never a float | `long` minor units everywhere; `BigDecimal` only for rates |
| Taxes are traceable | Each quote stores rule code and version |
| Events are never lost | Transactional outbox + idempotent consumers |
| The books stay healthy after every flow | `scripts/e2e-smoke.sh` checks the trial balance and invariants after each scenario |

---

## 17. Glossary

| Term | Meaning |
|---|---|
| Accrual | Recording revenue or expense when earned or incurred, not when cash moves |
| Allowance for credit losses | Losses expected on loans, set aside in advance (contra-asset) |
| Amortization | Repaying a loan in instalments split into interest and principal |
| Authorization | Network confirms the customer can pay; no money moves |
| Capture | Merchant confirms the sale; the payment becomes final |
| Chargeback | Customer's bank disputes a payment; the network pulls the money |
| Clearing account | Temporary account money passes through |
| Consolidation | Combining group entities' books after removing intercompany items |
| Contra account | An account that reduces another |
| Day-count convention | How days are counted for interest (ACT/365, ACT/360, 30/360) |
| DPD | Days past due |
| Functional currency | The currency an entity keeps its books in |
| Idempotency key | A unique key that makes a retry have no extra effect |
| IFRS 9 stage | Risk level of a loan that sets its allowance |
| Journal entry | One transaction with its debit and credit lines |
| Minor units | Smallest unit of a currency (cents) |
| Payable / receivable | What we owe / what is owed to us |
| Reconciliation | Checking our records against an outside source |
| Reserve | Merchant money held back for risk |
| Retained earnings | Profits kept from past periods |
| Reverse charge | Cross-border B2B VAT accounted for by the customer |
| Revaluation | Re-measuring foreign-currency balances at today's rate |
| Reversal | A new entry that cancels an earlier one |
| Safeguarding | Keeping customer money fully backed |
| Set-off | Using money in one account to pay a debt in another |
| Settlement | The network actually paying what it owes |
| Suspense | Where unexplained money waits |
| Trial balance | List of balances; total debits equal total credits |
| Waterfall | Order a repayment is applied in: fees, interest, principal |
| Withholding tax | Tax the payer holds back and pays to the authority |
| Write-off | Removing a debt that will not be paid and recording the loss |

---

## 18. Sources and further reading

Standards and principles:

- IFRS Foundation, [IFRS 9 Financial Instruments](https://www.ifrs.org/content/dam/ifrs/publications/html-standards/english/2024/issued/ifrs9.html) (expected credit losses, interest)
- IFRS Foundation, [list of standards](https://www.ifrs.org/issued-standards/list-of-standards/): IFRS 15 (revenue), IAS 21 (foreign currency), IFRS 10 (consolidation), IAS 1 (presentation)
- Bank for International Settlements, [IFRS 9 and expected loss provisioning](https://www.bis.org/fsi/fsisummaries/ifrs9.pdf) (plain-language summary of the three stages)
- US GAAP equivalents: ASC 830 (foreign currency), ASC 326 (current expected credit losses, "CECL")

For engineers:

- Martin Kleppmann, [Accounting for Computer Scientists](https://martin.kleppmann.com/2011/03/07/accounting-for-computer-scientists.html) (2011)
- Modern Treasury, Accounting for Developers: [Part I](https://www.moderntreasury.com/journal/accounting-for-developers-part-i), [Part II](https://www.moderntreasury.com/journal/accounting-for-developers-part-ii), [Part III: lending marketplace](https://www.moderntreasury.com/journal/accounting-for-developers-part-iii)
- Stripe, [Designing robust and predictable APIs with idempotency](https://stripe.com/blog/idempotency) and [APIs as infrastructure: versioning](https://stripe.com/blog/api-versioning)

Tax examples used in the seed data (verify before relying on them; rates change every year):

- PwC Worldwide Tax Summaries, [Kenya: other taxes](https://taxsummaries.pwc.com/kenya/corporate/other-taxes) (reviewed 17 July 2026)
- Business Daily, [M-Pesa, Airtel Money fees spared from 16pc VAT](https://www.businessdailyafrica.com/bd/economy/m-pesa-airtel-money-fees-spared-from-16-percent-vat-5501100) (18 June 2026)

Books for going further: *Accounting Made Simple* (Mike Piper) and *Financial Statements* (Thomas Ittelson) are
short, readable introductions.
