package com.collins.platform.commons.security;

/**
 * Thread-local storage for tenant context.
 *
 * This class holds the current user's tenant information (merchantId, individualId, role)
 * in ThreadLocal storage, ensuring thread-safety in a multi-tenant environment.
 *
 * The context is set by authentication filters and consumed by:
 * - Database connection customizers (to set PostgreSQL session variables)
 * - Repository queries (for application-level filtering)
 * - Audit logging
 *
 * @see TenantConnectionCustomizer
 */
public class TenantContext {

    private static final ThreadLocal<TenantInfo> CONTEXT = new ThreadLocal<>();

    /**
     * Sets the current tenant context.
     * Should be called by authentication filters after validating JWT.
     */
    public static void setTenantInfo(TenantInfo tenantInfo) {
        CONTEXT.set(tenantInfo);
    }

    /**
     * Gets the current tenant context.
     * Returns null if no tenant is set (e.g., public endpoints).
     */
    public static TenantInfo getTenantInfo() {
        return CONTEXT.get();
    }

    /**
     * Clears the current tenant context.
     * MUST be called in finally block to prevent context leakage between requests.
     */
    public static void clear() {
        CONTEXT.remove();
    }

    /**
     * Gets the current merchant ID.
     * Returns null if user is not a merchant.
     */
    public static String getCurrentMerchantId() {
        TenantInfo info = CONTEXT.get();
        return info != null ? info.getMerchantId() : null;
    }

    /**
     * Gets the current individual ID (customer).
     * Returns null if user is not a customer.
     */
    public static String getCurrentIndividualId() {
        TenantInfo info = CONTEXT.get();
        return info != null ? info.getIndividualId() : null;
    }

    /**
     * Gets the current user role.
     * Returns null if no tenant context is set.
     */
    public static String getCurrentRole() {
        TenantInfo info = CONTEXT.get();
        return info != null ? info.getRole() : null;
    }

    /**
     * Checks if current user is an admin.
     * Admins bypass tenant isolation (see RLS policies).
     */
    public static boolean isAdmin() {
        return "admin".equalsIgnoreCase(getCurrentRole());
    }

    /**
     * Checks if current user is a merchant.
     */
    public static boolean isMerchant() {
        return "merchant".equalsIgnoreCase(getCurrentRole());
    }

    /**
     * Checks if current user is a customer.
     */
    public static boolean isCustomer() {
        return "customer".equalsIgnoreCase(getCurrentRole());
    }
}
