-- Legal entities: one licensed company per country, each with its own books and functional currency.
create table legal_entities (
    entity_id           varchar(10) primary key,
    name                text        not null,
    country             varchar(2)  not null,
    functional_currency varchar(3)  not null,
    timezone            text        not null
);

-- Chart of accounts per entity. normal_side says which side increases the account.
create table chart_accounts (
    entity_id    varchar(10) not null references legal_entities (entity_id),
    code         varchar(10) not null,
    name         text        not null,
    account_type varchar(20) not null check (account_type in ('ASSET','CONTRA_ASSET','LIABILITY','EQUITY','REVENUE','EXPENSE')),
    normal_side  varchar(6)  not null check (normal_side in ('DEBIT','CREDIT')),
    primary key (entity_id, code)
);

-- Ledger accounts are created on first use: a base code plus an optional sub-ledger key, per currency.
create table ledger_accounts (
    id         bigserial primary key,
    entity_id  varchar(10)  not null references legal_entities (entity_id),
    account    varchar(120) not null,
    base_code  varchar(10)  not null,
    currency   varchar(3)   not null,
    created_at timestamptz  not null default now(),
    unique (entity_id, account, currency),
    foreign key (entity_id, base_code) references chart_accounts (entity_id, code)
);

-- Balance projection (CQRS-light). net = debits - credits. Rebuildable from ledger_entries.
create table account_balances (
    account_id  bigint primary key references ledger_accounts (id),
    posted_net  bigint      not null default 0,
    pending_net bigint      not null default 0,
    updated_at  timestamptz not null default now()
);

create table ledger_transactions (
    id               varchar(40)  primary key,
    entity_id        varchar(10)  not null references legal_entities (entity_id),
    idempotency_key  varchar(200) not null,
    request_hash     varchar(64)  not null,
    transaction_type varchar(60)  not null,
    description      text,
    effective_date   date         not null,
    reference_type   varchar(60),
    reference_id     varchar(120),
    status           varchar(10)  not null check (status in ('PENDING','POSTED','VOIDED')),
    reversal_of      varchar(40) references ledger_transactions (id),
    metadata_json    text,
    created_at       timestamptz  not null default now(),
    posted_at        timestamptz,
    voided_at        timestamptz,
    unique (entity_id, idempotency_key)
);
create index ix_ledger_transactions_reference on ledger_transactions (reference_id);
create index ix_ledger_transactions_entity_date on ledger_transactions (entity_id, effective_date);

-- Entries are append-only: never updated, never deleted (enforced by the trigger below).
create table ledger_entries (
    id             bigserial primary key,
    transaction_id varchar(40) not null references ledger_transactions (id),
    account_id     bigint      not null references ledger_accounts (id),
    line_no        int         not null,
    direction      varchar(6)  not null check (direction in ('DEBIT','CREDIT')),
    amount_minor   bigint      not null check (amount_minor > 0),
    currency       varchar(3)  not null
);
create index ix_ledger_entries_account on ledger_entries (account_id);
create index ix_ledger_entries_transaction on ledger_entries (transaction_id);

create or replace function forbid_entry_changes() returns trigger as $$
begin
    raise exception 'ledger_entries are append-only; post a reversal instead';
end;
$$ language plpgsql;

create trigger ledger_entries_append_only
    before update or delete on ledger_entries
    for each row execute function forbid_entry_changes();

create table accounting_periods (
    entity_id varchar(10) not null references legal_entities (entity_id),
    period    varchar(7)  not null,
    status    varchar(10) not null check (status in ('OPEN','CLOSED')),
    closed_at timestamptz,
    primary key (entity_id, period)
);

-- Last FX rate used to revalue each foreign-currency account (for unrealized gains and losses).
create table fx_revaluation_state (
    account_id bigint primary key references ledger_accounts (id),
    rate       numeric(20, 10) not null,
    as_of      date            not null
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
