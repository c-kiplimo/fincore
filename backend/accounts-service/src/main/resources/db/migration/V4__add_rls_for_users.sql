-- V4: Add Row-Level Security for users table

-- Enable RLS on users table
ALTER TABLE users ENABLE ROW LEVEL SECURITY;

-- Policy: users can only see their own record
CREATE POLICY user_isolation_users ON users
    FOR ALL
    USING (id = current_setting('app.current_user_id', true))
    WITH CHECK (id = current_setting('app.current_user_id', true));

-- Admin policy for users (admins can see all users)
CREATE POLICY admin_full_access_users ON users
    FOR ALL
    TO PUBLIC
    USING (current_setting('app.user_role', true) = 'ADMIN');
