-- Merchants module (own schema-level tables; could be extracted to its own service later).
create table merchants (
    id                  varchar(40) primary key,
    entity_id           varchar(10) not null,
    name                text        not null,
    country             varchar(2)  not null,
    settlement_currency varchar(3)  not null,
    reserve_percent_bp  int         not null default 0,
    reserve_days        int         not null default 0,
    status              varchar(12) not null default 'ACTIVE',
    created_at          timestamptz not null default now()
);

create table reserve_holds (
    id           varchar(40) primary key,
    merchant_id  varchar(40) not null references merchants (id),
    payment_id   varchar(40) not null unique,
    entity_id    varchar(10) not null,
    amount_minor bigint      not null,
    currency     varchar(3)  not null,
    release_on   date        not null,
    status       varchar(10) not null check (status in ('HELD','RELEASED')),
    created_at   timestamptz not null default now(),
    released_at  timestamptz
);

-- Wallets module.
create table individuals (
    id         varchar(40) primary key,
    entity_id  varchar(10) not null,
    name       text        not null,
    country    varchar(2)  not null,
    created_at timestamptz not null default now()
);

create table wallets (
    id             varchar(40) primary key,
    individual_id  varchar(40) not null references individuals (id),
    entity_id      varchar(10) not null,
    currency       varchar(3)  not null,
    kind           varchar(10) not null check (kind in ('MAIN','SAVINGS')),
    ledger_account varchar(60) not null,
    status         varchar(10) not null default 'ACTIVE',
    created_at     timestamptz not null default now(),
    unique (individual_id, currency, kind)
);

create table transfers (
    id                  varchar(40) primary key,
    transfer_type       varchar(30) not null,
    entity_id           varchar(10) not null,
    from_ref            varchar(60),
    to_ref              varchar(60),
    amount_minor        bigint      not null,
    currency            varchar(3)  not null,
    fee_minor           bigint      not null default 0,
    tax_minor           bigint      not null default 0,
    target_amount_minor bigint,
    target_currency     varchar(3),
    quote_id            varchar(40),
    ledger_txn_id       varchar(40),
    created_at          timestamptz not null default now()
);

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
