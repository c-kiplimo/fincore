-- Users table for authentication
create table users (
    id            varchar(40) primary key,
    email         varchar(255) not null unique,
    password_hash text         not null,
    name          text         not null,
    role          varchar(20)  not null check (role in ('ADMIN','MERCHANT','CUSTOMER')),
    tenant_id     varchar(10)  not null,
    created_at    timestamptz  not null default now(),
    updated_at    timestamptz  not null default now()
);

create index ix_users_email on users(email);
create index ix_users_tenant on users(tenant_id);
