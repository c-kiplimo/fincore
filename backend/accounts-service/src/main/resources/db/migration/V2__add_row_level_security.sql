-- V2: Add Row-Level Security for Multitenancy
-- This migration adds PostgreSQL Row-Level Security (RLS) to ensure tenant isolation

-- ===== MERCHANTS MODULE =====

-- Enable RLS on merchants table
ALTER TABLE merchants ENABLE ROW LEVEL SECURITY;

-- Policy: merchants can only see their own record
CREATE POLICY merchant_isolation_merchants ON merchants
    FOR ALL
    USING (id = current_setting('app.current_merchant_id', true))
    WITH CHECK (id = current_setting('app.current_merchant_id', true));

-- Admin policy for merchants
CREATE POLICY admin_full_access_merchants ON merchants
    FOR ALL
    TO PUBLIC
    USING (current_setting('app.user_role', true) = 'admin');

-- Enable RLS on reserve_holds table
ALTER TABLE reserve_holds ENABLE ROW LEVEL SECURITY;

-- Policy: merchants can only see their own reserve holds
CREATE POLICY merchant_isolation_reserve_holds ON reserve_holds
    FOR ALL
    USING (merchant_id = current_setting('app.current_merchant_id', true))
    WITH CHECK (merchant_id = current_setting('app.current_merchant_id', true));

-- Admin policy for reserve_holds
CREATE POLICY admin_full_access_reserve_holds ON reserve_holds
    FOR ALL
    TO PUBLIC
    USING (current_setting('app.user_role', true) = 'admin');

-- Create index for merchant_id on reserve_holds
CREATE INDEX ix_reserve_holds_merchant ON reserve_holds (merchant_id);
COMMENT ON INDEX ix_reserve_holds_merchant IS 'Index used by Row-Level Security policies for tenant isolation';

-- ===== WALLETS MODULE (Customer Multitenancy) =====

-- Enable RLS on individuals table
ALTER TABLE individuals ENABLE ROW LEVEL SECURITY;

-- Policy: individuals can only see their own record
CREATE POLICY individual_isolation_individuals ON individuals
    FOR ALL
    USING (id = current_setting('app.current_individual_id', true))
    WITH CHECK (id = current_setting('app.current_individual_id', true));

-- Admin policy for individuals
CREATE POLICY admin_full_access_individuals ON individuals
    FOR ALL
    TO PUBLIC
    USING (current_setting('app.user_role', true) = 'admin');

-- Enable RLS on wallets table
ALTER TABLE wallets ENABLE ROW LEVEL SECURITY;

-- Policy: individuals can only see their own wallets
CREATE POLICY individual_isolation_wallets ON wallets
    FOR ALL
    USING (individual_id = current_setting('app.current_individual_id', true))
    WITH CHECK (individual_id = current_setting('app.current_individual_id', true));

-- Admin policy for wallets
CREATE POLICY admin_full_access_wallets ON wallets
    FOR ALL
    TO PUBLIC
    USING (current_setting('app.user_role', true) = 'admin');

-- Create index for individual_id on wallets
CREATE INDEX ix_wallets_individual ON wallets (individual_id);
COMMENT ON INDEX ix_wallets_individual IS 'Index used by Row-Level Security policies for tenant isolation';

-- Enable RLS on transfers table
ALTER TABLE transfers ENABLE ROW LEVEL SECURITY;

-- Policy: users can see transfers involving their wallets
-- This is complex because transfers can be from or to a wallet
CREATE POLICY individual_isolation_transfers ON transfers
    FOR ALL
    USING (
        -- Can see if from_ref contains their wallet
        from_ref IN (
            SELECT ledger_account FROM wallets
            WHERE individual_id = current_setting('app.current_individual_id', true)
        )
        OR
        -- Can see if to_ref contains their wallet
        to_ref IN (
            SELECT ledger_account FROM wallets
            WHERE individual_id = current_setting('app.current_individual_id', true)
        )
    );

-- Admin policy for transfers
CREATE POLICY admin_full_access_transfers ON transfers
    FOR ALL
    TO PUBLIC
    USING (current_setting('app.user_role', true) = 'admin');

-- Note: idempotency_keys, outbox_events, processed_events are infrastructure tables, no RLS needed
