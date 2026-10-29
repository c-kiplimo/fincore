package com.collins.platform.commons.config;

import com.collins.platform.commons.security.TenantContext;
import com.collins.platform.commons.security.TenantInfo;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Sets PostgreSQL session variables for Row-Level Security (RLS).
 *
 * This class hooks into database connection creation/retrieval and sets
 * session variables that RLS policies use for tenant isolation:
 * - app.current_merchant_id
 * - app.current_individual_id
 * - app.user_role
 *
 * These variables are read by PostgreSQL RLS policies to filter rows.
 *
 * @see com.collins.platform.commons.security.TenantContext
 */
@Component
public class TenantConnectionCustomizer {

    /**
     * Sets tenant context on a database connection.
     * Call this BEFORE executing any queries.
     *
     * This method is safe to call multiple times (idempotent).
     */
    public static void setTenantContext(Connection connection) throws SQLException {
        TenantInfo tenant = TenantContext.getTenantInfo();

        if (tenant == null) {
            // No tenant context (e.g., public endpoint, system task)
            // Set role to 'anonymous' so RLS policies can handle it
            setSessionVariable(connection, "app.user_role", "anonymous");
            setSessionVariable(connection, "app.current_merchant_id", "");
            setSessionVariable(connection, "app.current_individual_id", "");
            return;
        }

        // Set user role (admin, merchant, customer)
        setSessionVariable(connection, "app.user_role", tenant.getRole());

        // Set merchant ID if merchant
        if (tenant.getMerchantId() != null) {
            setSessionVariable(connection, "app.current_merchant_id", tenant.getMerchantId());
        } else {
            setSessionVariable(connection, "app.current_merchant_id", "");
        }

        // Set individual ID if customer
        if (tenant.getIndividualId() != null) {
            setSessionVariable(connection, "app.current_individual_id", tenant.getIndividualId());
        } else {
            setSessionVariable(connection, "app.current_individual_id", "");
        }
    }

    /**
     * Sets a PostgreSQL session variable using SET LOCAL.
     * SET LOCAL ensures the variable is transaction-scoped (reset after commit/rollback).
     */
    private static void setSessionVariable(Connection connection, String name, String value) throws SQLException {
        String sql = String.format("SET LOCAL %s = ?", name);
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, value);
            stmt.execute();
        }
    }

    /**
     * Clears tenant context from connection.
     * Should be called when returning connection to pool.
     */
    public static void clearTenantContext(Connection connection) throws SQLException {
        setSessionVariable(connection, "app.user_role", "");
        setSessionVariable(connection, "app.current_merchant_id", "");
        setSessionVariable(connection, "app.current_individual_id", "");
    }
}
