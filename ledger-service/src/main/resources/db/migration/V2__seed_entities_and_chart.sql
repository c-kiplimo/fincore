insert into legal_entities (entity_id, name, country, functional_currency, timezone) values
    ('KE', 'Platform KE Ltd', 'KE', 'KES', 'Africa/Nairobi'),
    ('UK', 'Platform UK Ltd', 'GB', 'GBP', 'Europe/London'),
    ('US', 'Platform US Inc', 'US', 'USD', 'America/New_York');

-- The same chart of accounts for every entity. Sub-ledgers (e.g. 2000:mer_123) are created on first use.
insert into chart_accounts (entity_id, code, name, account_type, normal_side)
select e.entity_id, c.code, c.name, c.account_type, c.normal_side
from legal_entities e
cross join (values
    ('1000', 'Cash at bank',                     'ASSET',        'DEBIT'),
    ('1100', 'Settlement receivable',            'ASSET',        'DEBIT'),
    ('1200', 'Merchant advance receivable',      'ASSET',        'DEBIT'),
    ('1210', 'Unearned financing fee',           'CONTRA_ASSET', 'CREDIT'),
    ('1220', 'Allowance for credit losses',      'CONTRA_ASSET', 'CREDIT'),
    ('1230', 'Loan receivable',                  'ASSET',        'DEBIT'),
    ('1240', 'Interest receivable',              'ASSET',        'DEBIT'),
    ('1250', 'Loan fees receivable',             'ASSET',        'DEBIT'),
    ('1300', 'Merchant receivable',              'ASSET',        'DEBIT'),
    ('1310', 'Customer receivable',              'ASSET',        'DEBIT'),
    ('1400', 'FX clearing',                      'ASSET',        'DEBIT'),
    ('1410', 'FX revaluation adjustment',        'ASSET',        'DEBIT'),
    ('1600', 'Intercompany receivable',          'ASSET',        'DEBIT'),
    ('2000', 'Merchant payable',                 'LIABILITY',    'CREDIT'),
    ('2010', 'Customer wallet',                  'LIABILITY',    'CREDIT'),
    ('2100', 'Merchant reserve',                 'LIABILITY',    'CREDIT'),
    ('2200', 'Disputed funds held',              'LIABILITY',    'CREDIT'),
    ('2300', 'Payout clearing',                  'LIABILITY',    'CREDIT'),
    ('2400', 'Accrued expenses',                 'LIABILITY',    'CREDIT'),
    ('2500', 'Excise duty payable',              'LIABILITY',    'CREDIT'),
    ('2510', 'VAT / GST payable',                'LIABILITY',    'CREDIT'),
    ('2520', 'Withholding tax payable',          'LIABILITY',    'CREDIT'),
    ('2600', 'Intercompany payable',             'LIABILITY',    'CREDIT'),
    ('2900', 'Suspense',                         'LIABILITY',    'CREDIT'),
    ('3000', 'Owner''s equity',                  'EQUITY',       'CREDIT'),
    ('3100', 'Retained earnings',                'EQUITY',       'CREDIT'),
    ('3200', 'FX translation reserve',           'EQUITY',       'CREDIT'),
    ('4000', 'Payment fee revenue',              'REVENUE',      'CREDIT'),
    ('4100', 'Financing fee revenue',            'REVENUE',      'CREDIT'),
    ('4110', 'Interest income',                  'REVENUE',      'CREDIT'),
    ('4120', 'Late fee income',                  'REVENUE',      'CREDIT'),
    ('4200', 'Dispute fee revenue',              'REVENUE',      'CREDIT'),
    ('4300', 'Transfer and withdrawal fee revenue', 'REVENUE',   'CREDIT'),
    ('4500', 'FX spread revenue',                'REVENUE',      'CREDIT'),
    ('4600', 'Intercompany service revenue',     'REVENUE',      'CREDIT'),
    ('4900', 'FX gain',                          'REVENUE',      'CREDIT'),
    ('5000', 'Network fees expense',             'EXPENSE',      'DEBIT'),
    ('5100', 'Credit loss expense',              'EXPENSE',      'DEBIT'),
    ('5200', 'FX loss',                          'EXPENSE',      'DEBIT'),
    ('5300', 'Chargeback loss',                  'EXPENSE',      'DEBIT'),
    ('5400', 'Bank charges',                     'EXPENSE',      'DEBIT'),
    ('5600', 'Intercompany service expense',     'EXPENSE',      'DEBIT')
) as c(code, name, account_type, normal_side);
