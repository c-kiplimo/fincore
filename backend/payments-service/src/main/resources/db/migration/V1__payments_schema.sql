create table payment_intents (
    id              varchar(40) primary key,
    entity_id       varchar(10) not null,
    merchant_id     varchar(40) not null,
    amount_minor    bigint      not null check (amount_minor > 0),
    currency        varchar(3)  not null,
    method          varchar(20) not null check (method in ('CARD','MOBILE_MONEY')),
    status          varchar(24) not null,
    network_ref     varchar(60),
    decline_reason  varchar(60),
    captured_minor  bigint      not null default 0,
    refunded_minor  bigint      not null default 0,
    fee_minor       bigint      not null default 0,
    tax_minor       bigint      not null default 0,
    net_minor       bigint      not null default 0,
    quote_id        varchar(40),
    description     text,
    created_at      timestamptz not null default now(),
    updated_at      timestamptz not null default now()
);
create index ix_payment_intents_merchant on payment_intents (merchant_id);

create table refunds (
    id            varchar(40) primary key,
    payment_id    varchar(40) not null references payment_intents (id),
    amount_minor  bigint      not null check (amount_minor > 0),
    reason        text,
    ledger_txn_id varchar(40),
    created_at    timestamptz not null default now()
);

create table disputes (
    id           varchar(40) primary key,
    payment_id   varchar(40) not null references payment_intents (id),
    amount_minor bigint      not null,
    fee_minor    bigint      not null,
    status       varchar(10) not null check (status in ('OPEN','WON','LOST')),
    reason       text,
    opened_at    timestamptz not null default now(),
    resolved_at  timestamptz
);

create table settlement_batches (
    batch_id      varchar(40) primary key,
    entity_id     varchar(10) not null,
    currency      varchar(3)  not null,
    settled_on    date        not null,
    gross_minor   bigint      not null,
    fees_minor    bigint      not null,
    net_minor     bigint      not null,
    line_count    int         not null,
    ledger_txn_id varchar(40),
    created_at    timestamptz not null default now()
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
