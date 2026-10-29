create table payouts (
    id                  varchar(40) primary key,
    entity_id           varchar(10) not null,
    beneficiary_type    varchar(10) not null check (beneficiary_type in ('MERCHANT','WALLET')),
    beneficiary_id      varchar(40) not null,
    source_account      varchar(60) not null,
    amount_minor        bigint      not null check (amount_minor > 0),
    currency            varchar(3)  not null,
    fee_minor           bigint      not null default 0,
    tax_minor           bigint      not null default 0,
    total_debited_minor bigint      not null,
    destination_type    varchar(20) not null,
    destination_account varchar(60) not null,
    status              varchar(12) not null check (status in ('CREATED','SENDING','SENT','PAID','FAILED')),
    pending_txn_id      varchar(40),
    confirm_txn_id      varchar(40),
    bank_ref            varchar(60),
    failure_reason      text,
    attempts            int         not null default 0,
    created_at          timestamptz not null default now(),
    updated_at          timestamptz not null default now()
);
create index ix_payouts_status on payouts (status);

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
