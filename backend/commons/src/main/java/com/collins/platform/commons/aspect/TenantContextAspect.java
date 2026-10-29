package com.collins.platform.commons.aspect;

import com.collins.platform.commons.config.TenantConnectionCustomizer;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.sql.DataSource;
import java.sql.Connection;

/**
 * AOP Aspect that sets PostgreSQL session variables for Row-Level Security.
 *
 * This aspect intercepts all @Transactional methods and sets the tenant context
 * on the database connection BEFORE the transaction begins.
 *
 * This ensures that RLS policies have access to the current tenant ID via
 * session variables like app.current_merchant_id.
 */
@Aspect
@Component
public class TenantContextAspect {

    private static final Logger log = LoggerFactory.getLogger(TenantContextAspect.class);

    private final DataSource dataSource;

    public TenantContextAspect(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * Intercepts all @Transactional methods to set tenant context.
     */
    @Around("@annotation(org.springframework.transaction.annotation.Transactional) || " +
            "@within(org.springframework.transaction.annotation.Transactional)")
    public Object setTenantContextOnTransaction(ProceedingJoinPoint joinPoint) throws Throwable {
        // Only set tenant context if we're in a transaction
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            return joinPoint.proceed();
        }

        Connection connection = null;
        boolean shouldReleaseConnection = false;

        try {
            // Get the current connection from the transaction
            connection = DataSourceUtils.getConnection(dataSource);
            shouldReleaseConnection = false; // Spring manages it

            // Set PostgreSQL session variables for RLS
            TenantConnectionCustomizer.setTenantContext(connection);

            log.debug("Set tenant context on database connection for transaction");

            // Proceed with the original method
            return joinPoint.proceed();

        } catch (Exception e) {
            log.error("Error setting tenant context on transaction", e);
            throw e;
        } finally {
            // Don't release connection - Spring's transaction manager handles it
            // Connection is automatically returned to pool after transaction commits/rolls back
        }
    }
}
