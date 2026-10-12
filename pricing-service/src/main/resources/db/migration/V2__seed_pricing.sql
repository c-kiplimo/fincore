insert into entities (entity_id, country, functional_currency) values
    ('KE', 'KE', 'KES'), ('UK', 'GB', 'GBP'), ('US', 'US', 'USD');

-- Example fee plans (learning values, not real prices).
insert into fee_plans (entity_id, product, percent_bp, fixed_minor, fee_payer, valid_from) values
    ('KE', 'CARD_PAYMENT',    290, 0,    'RECIPIENT', '2025-01-01'),
    ('KE', 'WALLET_PAYMENT',  150, 0,    'RECIPIENT', '2025-01-01'),
    ('KE', 'P2P_TRANSFER',    0,   1000, 'PAYER',     '2025-01-01'),
    ('KE', 'WITHDRAWAL',      0,   3000, 'PAYER',     '2025-01-01'),
    ('KE', 'MERCHANT_PAYOUT', 0,   0,    'PAYER',     '2025-01-01'),
    ('KE', 'FX_CONVERSION',   0,   0,    'PAYER',     '2025-01-01'),
    ('UK', 'CARD_PAYMENT',    150, 20,   'RECIPIENT', '2025-01-01'),
    ('UK', 'WALLET_PAYMENT',  100, 0,    'RECIPIENT', '2025-01-01'),
    ('UK', 'P2P_TRANSFER',    0,   0,    'PAYER',     '2025-01-01'),
    ('UK', 'WITHDRAWAL',      0,   100,  'PAYER',     '2025-01-01'),
    ('UK', 'MERCHANT_PAYOUT', 0,   0,    'PAYER',     '2025-01-01'),
    ('UK', 'FX_CONVERSION',   0,   0,    'PAYER',     '2025-01-01'),
    ('US', 'CARD_PAYMENT',    290, 30,   'RECIPIENT', '2025-01-01'),
    ('US', 'WALLET_PAYMENT',  100, 0,    'RECIPIENT', '2025-01-01'),
    ('US', 'P2P_TRANSFER',    0,   0,    'PAYER',     '2025-01-01'),
    ('US', 'WITHDRAWAL',      0,   0,    'PAYER',     '2025-01-01'),
    ('US', 'MERCHANT_PAYOUT', 0,   0,    'PAYER',     '2025-01-01'),
    ('US', 'FX_CONVERSION',   0,   0,    'PAYER',     '2025-01-01');

-- Kenya rows follow published 2026 rules (verify before real use). UK row is an illustrative example only.
insert into tax_rules (rule_code, version, jurisdiction, product, tax_type, rate, ledger_account, reverse_charge_b2b,
                       valid_from, example, description, source_url) values
    ('KE-EXC-P2P', 1, 'KE', 'P2P_TRANSFER', 'EXCISE', 0.15, '2500', false, '2025-01-01', false,
     'Excise duty 15% on money-transfer fees', 'https://taxsummaries.pwc.com/kenya/corporate/other-taxes'),
    ('KE-EXC-WDR', 1, 'KE', 'WITHDRAWAL', 'EXCISE', 0.15, '2500', false, '2025-01-01', false,
     'Excise duty 15% on money-transfer fees', 'https://taxsummaries.pwc.com/kenya/corporate/other-taxes'),
    ('KE-VAT-ACQ', 1, 'KE', 'CARD_PAYMENT', 'VAT', 0.16, '2510', false, '2026-07-01', false,
     'VAT 16% on payment processing / merchant acquiring by PSPs (Finance Act 2026)', 'https://taxsummaries.pwc.com/kenya/corporate/other-taxes'),
    ('KE-VAT-WPAY', 1, 'KE', 'WALLET_PAYMENT', 'VAT', 0.16, '2510', false, '2026-07-01', false,
     'VAT 16% on payment processing by PSPs (Finance Act 2026)', 'https://taxsummaries.pwc.com/kenya/corporate/other-taxes'),
    ('GB-VAT-ACQ-EXAMPLE', 1, 'GB', 'CARD_PAYMENT', 'VAT', 0.20, '2510', true, '2025-01-01', true,
     'ILLUSTRATIVE ONLY: shows VAT with reverse charge for cross-border B2B. Many UK payment services are VAT-exempt; verify.', null);

-- Example FX rates (not market data). Inverse pairs are derived automatically.
insert into fx_rates (base, quote, mid_rate, spread_bp) values
    ('USD', 'KES', 130.0000000000, 100),
    ('GBP', 'KES', 165.0000000000, 100),
    ('USD', 'GBP', 0.7900000000, 50);

insert into interest_rules (jurisdiction, product, day_count, max_annual_rate_bp, disclosure) values
    ('KE', 'INSTALMENT_LOAN',    'ACT_365', null, 'Show total cost of credit before acceptance'),
    ('KE', 'MERCHANT_FINANCING', 'ACT_365', null, 'Show fixed fee and withholding rate'),
    ('GB', 'INSTALMENT_LOAN',    'ACT_365', null, 'Show APR (illustrative)'),
    ('US', 'INSTALMENT_LOAN',    'ACT_360', null, 'Show APR (illustrative)');
