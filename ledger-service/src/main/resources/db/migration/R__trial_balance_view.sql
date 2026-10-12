-- Repeatable migration: re-runs whenever this file changes.
create or replace view v_posted_entries as
select t.entity_id, t.id as transaction_id, t.effective_date, t.transaction_type,
       la.account, la.base_code, la.currency, e.direction, e.amount_minor,
       case when e.direction = 'DEBIT' then e.amount_minor else -e.amount_minor end as net_minor
from ledger_entries e
join ledger_transactions t on t.id = e.transaction_id
join ledger_accounts la on la.id = e.account_id
where t.status = 'POSTED';
