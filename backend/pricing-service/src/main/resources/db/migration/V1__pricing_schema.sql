-- Reference copy of legal entities (each service owns its own data; this is pricing's view).
create table entities (
    entity_id           varchar(10) primary key,
    country             varchar(2)  not null,
    functional_currency varchar(3)  not null
);

-- Fees per entity and product. fee_payer: PAYER adds the fee on top; RECIPIENT has it deducted.
create table fee_plans (
    id          bigserial primary key,
    entity_id   varchar(10) not null references entities (entity_id),
    product     varchar(40) not null,
    percent_bp  int         not null default 0,
    fixed_minor bigint      not null default 0,
    min_minor   bigint      not null default 0,
    max_minor   bigint,
    fee_payer   varchar(10) not null check (fee_payer in ('PAYER','RECIPIENT')),
    valid_from  date        not null,
    valid_to    date
);

-- Tax rules are data with effective dates: a Finance Act change is a new version row, not a code change.
create table tax_rules (
    id                    bigserial primary key,
    rule_code             varchar(40)   not null,
    version               int           not null,
    jurisdiction          varchar(2)    not null,
    product               varchar(40)   not null,
    tax_type              varchar(20)   not null check (tax_type in ('VAT','GST','EXCISE','SALES_TAX','WITHHOLDING')),
    base                  varchar(10)   not null default 'FEE',
    rate                  numeric(9, 6) not null,
    ledger_account        varchar(10)   not null,
    reverse_charge_b2b    boolean       not null default false,
    valid_from            date          not null,
    valid_to              date,
    example               boolean       not null default false,
    description           text,
    source_url            text,
    unique (rule_code, version)
);

create table fx_rates (
    base       varchar(3)      not null,
    quote      varchar(3)      not null,
    mid_rate   numeric(20, 10) not null,
    spread_bp  int             not null default 0,
    as_of      timestamptz     not null default now(),
    primary key (base, quote)
);

create table interest_rules (
    jurisdiction       varchar(2)  not null,
    product            varchar(40) not null,
    day_count          varchar(12) not null check (day_count in ('ACT_365','ACT_360','THIRTY_360')),
    max_annual_rate_bp int,
    disclosure         text,
    primary key (jurisdiction, product)
);

create table quotes (
    id           varchar(40) primary key,
    entity_id    varchar(10) not null,
    product      varchar(40) not null,
    quote_json   text        not null,
    status       varchar(10) not null check (status in ('OPEN','USED')),
    expires_at   timestamptz not null,
    used_at      timestamptz,
    used_by      varchar(200),
    created_at   timestamptz not null default now()
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
