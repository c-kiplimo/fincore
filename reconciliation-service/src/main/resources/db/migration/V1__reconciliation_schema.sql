create table recon_runs (
    id             varchar(40) primary key,
    entity_id      varchar(10) not null,
    source         varchar(10) not null check (source in ('BANK','NETWORK')),
    currency       varchar(3)  not null,
    statement_date date        not null,
    matched        int         not null default 0,
    exceptions     int         not null default 0,
    created_at     timestamptz not null default now()
);

create table statement_lines (
    id             varchar(40) primary key,
    run_id         varchar(40) not null references recon_runs (id),
    external_ref   varchar(80) not null,
    amount_minor   bigint      not null,
    currency       varchar(3)  not null,
    value_date     date        not null,
    description    text,
    status         varchar(12) not null check (status in ('MATCHED','EXCEPTION','SUSPENSE','RESOLVED')),
    matched_txn_id varchar(40),
    explanation    text,
    suspense_txn_id varchar(40),
    created_at     timestamptz not null default now()
);
create index ix_statement_lines_run on statement_lines (run_id);

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
