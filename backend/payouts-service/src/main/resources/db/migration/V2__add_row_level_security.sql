-- V2: Add Row-Level Security for Multitenancy
-- This migration adds PostgreSQL Row-Level Security (RLS) to ensure tenant isolation

-- Enable RLS on payouts table
ALTER TABLE payouts ENABLE ROW LEVEL SECURITY;

-- Policy: merchants can only see payouts where they are the beneficiary
CREATE POLICY merchant_isolation_payouts ON payouts
    FOR ALL
    USING (
        beneficiary_type = 'MERCHANT'
        AND beneficiary_id = current_setting('app.current_merchant_id', true)
    )
    WITH CHECK (
        beneficiary_type = 'MERCHANT'
        AND beneficiary_id = current_setting('app.current_merchant_id', true)
    );

-- Policy: individuals can only see payouts where their wallet is the beneficiary
CREATE POLICY individual_isolation_payouts ON payouts
    FOR ALL
    USING (
        beneficiary_type = 'WALLET'
        AND beneficiary_id IN (
            -- This assumes we have a way to map wallet to individual
            -- In practice, this would need to join with accounts-service wallets table
            -- For now, we'll use a simple check
            SELECT current_setting('app.current_wallet_id', true)
        )
    )
    WITH CHECK (
        beneficiary_type = 'WALLET'
        AND beneficiary_id IN (
            SELECT current_setting('app.current_wallet_id', true)
        )
    );

-- Admin policy for payouts
CREATE POLICY admin_full_access_payouts ON payouts
    FOR ALL
    TO PUBLIC
    USING (current_setting('app.user_role', true) = 'admin');

-- Create indexes for beneficiary filtering
CREATE INDEX ix_payouts_beneficiary ON payouts (beneficiary_type, beneficiary_id);
COMMENT ON INDEX ix_payouts_beneficiary IS 'Index used by Row-Level Security policies for tenant isolation';

CREATE INDEX ix_payouts_beneficiary_merchant ON payouts (beneficiary_id)
    WHERE beneficiary_type = 'MERCHANT';
COMMENT ON INDEX ix_payouts_beneficiary_merchant IS 'Partial index for merchant payouts (RLS optimization)';

-- Note: idempotency_keys, outbox_events, processed_events are infrastructure tables, no RLS needed
