-- V2: Add Row-Level Security for Multitenancy
-- This migration adds PostgreSQL Row-Level Security (RLS) to ensure tenant isolation

-- Enable Row-Level Security on payment_intents table
ALTER TABLE payment_intents ENABLE ROW LEVEL SECURITY;

-- Create policy for payment_intents: merchants can only see their own payments
CREATE POLICY merchant_isolation_payment_intents ON payment_intents
    FOR ALL
    USING (merchant_id = current_setting('app.current_merchant_id', true))
    WITH CHECK (merchant_id = current_setting('app.current_merchant_id', true));

-- Allow platform admins to see all records (bypass RLS)
-- This policy allows users with admin role to bypass tenant isolation
CREATE POLICY admin_full_access_payment_intents ON payment_intents
    FOR ALL
    TO PUBLIC
    USING (current_setting('app.user_role', true) = 'admin');

-- Enable RLS on refunds table
ALTER TABLE refunds ENABLE ROW LEVEL SECURITY;

-- Policy for refunds: filter by merchant_id through payment_intents FK
CREATE POLICY merchant_isolation_refunds ON refunds
    FOR ALL
    USING (
        payment_id IN (
            SELECT id FROM payment_intents
            WHERE merchant_id = current_setting('app.current_merchant_id', true)
        )
    )
    WITH CHECK (
        payment_id IN (
            SELECT id FROM payment_intents
            WHERE merchant_id = current_setting('app.current_merchant_id', true)
        )
    );

-- Admin policy for refunds
CREATE POLICY admin_full_access_refunds ON refunds
    FOR ALL
    TO PUBLIC
    USING (current_setting('app.user_role', true) = 'admin');

-- Enable RLS on disputes table
ALTER TABLE disputes ENABLE ROW LEVEL SECURITY;

-- Policy for disputes: filter by merchant_id through payment_intents FK
CREATE POLICY merchant_isolation_disputes ON disputes
    FOR ALL
    USING (
        payment_id IN (
            SELECT id FROM payment_intents
            WHERE merchant_id = current_setting('app.current_merchant_id', true)
        )
    )
    WITH CHECK (
        payment_id IN (
            SELECT id FROM payment_intents
            WHERE merchant_id = current_setting('app.current_merchant_id', true)
        )
    );

-- Admin policy for disputes
CREATE POLICY admin_full_access_disputes ON disputes
    FOR ALL
    TO PUBLIC
    USING (current_setting('app.user_role', true) = 'admin');

-- Note: settlement_batches is platform-wide, no RLS needed
-- Note: idempotency_keys, outbox_events, processed_events are infrastructure tables, no RLS needed

-- Create index to optimize RLS policy queries
-- merchant_id index already exists from V1, but add comment
COMMENT ON INDEX ix_payment_intents_merchant IS 'Index used by Row-Level Security policies for tenant isolation';
