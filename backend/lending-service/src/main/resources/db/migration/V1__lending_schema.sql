-- Loans module ----------------------------------------------------------------
create table loan_products (
    code                varchar(30) primary key,
    kind                varchar(20) not null check (kind in ('MERCHANT_FINANCING','INSTALMENT_LOAN')),
    name                text        not null,
    annual_rate_bp      int         not null default 0,
    term_months         int         not null default 0,
    fixed_fee_bp        int         not null default 0,
    withhold_bp         int         not null default 0,
    late_fee_minor      bigint      not null default 0,
    grace_days          int         not null default 0,
    max_principal_minor bigint      not null
);

create table loans (
    id                  varchar(40) primary key,
    entity_id           varchar(10) not null,
    jurisdiction        varchar(2)  not null,
    product_code        varchar(30) not null references loan_products (code),
    kind                varchar(20) not null,
    borrower_type       varchar(12) not null check (borrower_type in ('INDIVIDUAL','MERCHANT')),
    borrower_id         varchar(40) not null,
    borrower_account    varchar(60) not null,
    currency            varchar(3)  not null,
    principal_minor     bigint      not null,
    fee_minor           bigint      not null default 0,
    annual_rate_bp      int         not null default 0,
    term_months         int         not null default 0,
    status              varchar(12) not null,
    stage               int         not null default 1,
    dpd                 int         not null default 0,
    outstanding_minor   bigint      not null default 0,  -- principal (1230) or total receivable (1200)
    interest_due_minor  bigint      not null default 0,  -- accrued, unpaid interest (1240)
    fees_due_minor      bigint      not null default 0,  -- unpaid late fees (1250)
    unearned_fee_minor  bigint      not null default 0,  -- merchant financing fee not yet earned (1210)
    allowance_minor     bigint      not null default 0,  -- allowance for credit losses (1220)
    disbursed_on        date,
    last_accrual_date   date,
    created_at          timestamptz not null default now(),
    updated_at          timestamptz not null default now()
);
create index ix_loans_borrower on loans (borrower_id);
create index ix_loans_status on loans (status);

create table loan_schedule (
    loan_id        varchar(40) not null references loans (id),
    n              int         not null,
    due_date       date        not null,
    principal_due  bigint      not null,
    interest_due   bigint      not null,
    paid_minor     bigint      not null default 0,
    late_fee_done  boolean     not null default false,
    primary key (loan_id, n)
);

create table loan_repayments (
    id             varchar(40) primary key,
    loan_id        varchar(40) not null references loans (id),
    amount_minor   bigint      not null,
    fees_part      bigint      not null,
    interest_part  bigint      not null,
    principal_part bigint      not null,
    source_account varchar(60) not null,
    ledger_txn_id  varchar(40),
    created_at     timestamptz not null default now()
);

-- Collections module -------------------------------------------------------------
create table recovery_cases (
    id             varchar(40) primary key,
    entity_id      varchar(10) not null,
    debtor_type    varchar(12) not null check (debtor_type in ('INDIVIDUAL','MERCHANT')),
    debtor_id      varchar(40) not null,
    debt_kind      varchar(12) not null check (debt_kind in ('BALANCE','LOAN')),
    debt_account   varchar(60) not null,
    currency       varchar(3)  not null,
    amount_minor   bigint      not null,
    recovered_minor bigint     not null default 0,
    written_off_minor bigint   not null default 0,
    status         varchar(24) not null,
    reason         text,
    reference_id   varchar(60),
    created_at     timestamptz not null default now(),
    updated_at     timestamptz not null default now()
);

create table recovery_steps (
    id                varchar(40) primary key,
    case_id           varchar(40) not null references recovery_cases (id),
    step_type         varchar(20) not null,
    source_account    varchar(60) not null,
    source_currency   varchar(3)  not null,
    source_minor      bigint      not null,
    debt_minor        bigint      not null,
    fx_rate           numeric(20, 10),
    ledger_txn_id     varchar(40),
    created_at        timestamptz not null default now()
);

insert into loan_products (code, kind, name, annual_rate_bp, term_months, fixed_fee_bp, withhold_bp, late_fee_minor, grace_days, max_principal_minor) values
    ('MF_STANDARD', 'MERCHANT_FINANCING', 'Merchant financing: fixed 10% fee, 10% of each sale', 0, 0, 1000, 1000, 0, 0, 1000000000),
    ('PL_12M',      'INSTALMENT_LOAN',    'Personal loan: 24% a year, 12 monthly instalments', 2400, 12, 0, 0, 20000, 5, 50000000);

-- Shared platform tables (commons): idempotency, outbox, idempotent consumers.
create table idempotency_keys (
    scope         varchar(64)  not null,
    idem_key      varchar(200) not null,
    request_hash  varchar(64)  not null,
    status        varchar(16)  not null,
    response_json text,
    created_at    timestamptz  not null default now(),
    primary key (scope, idem_key)
);

create table outbox_events (
    id           uuid primary key,
    topic        varchar(200) not null,
    msg_key      varchar(200),
    event_type   varchar(100) not null,
    payload_json text         not null,
    created_at   timestamptz  not null default now(),
    published_at timestamptz
);
create index ix_outbox_unpublished on outbox_events (created_at) where published_at is null;

create table processed_events (
    consumer     varchar(100) not null,
    event_id     varchar(100) not null,
    processed_at timestamptz  not null default now(),
    primary key (consumer, event_id)
);
