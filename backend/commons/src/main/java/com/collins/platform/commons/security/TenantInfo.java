package com.collins.platform.commons.security;

/**
 * Holds tenant information extracted from JWT.
 *
 * This immutable class represents the tenant context for the current request.
 * It's stored in ThreadLocal via {@link TenantContext}.
 *
 * Exactly ONE of merchantId or individualId should be set (never both):
 * - If role=merchant: merchantId is set, individualId is null
 * - If role=customer: individualId is set, merchantId is null
 * - If role=admin: both can be null (admins are not tenants)
 */
public class TenantInfo {

    private final String merchantId;
    private final String individualId;
    private final String role;
    private final String userId; // JWT subject (user ID from auth system)

    private TenantInfo(String merchantId, String individualId, String role, String userId) {
        this.merchantId = merchantId;
        this.individualId = individualId;
        this.role = role;
        this.userId = userId;
    }

    /**
     * Creates TenantInfo for a merchant user.
     */
    public static TenantInfo forMerchant(String merchantId, String userId) {
        if (merchantId == null || merchantId.isBlank()) {
            throw new IllegalArgumentException("merchantId cannot be null or blank");
        }
        return new TenantInfo(merchantId, null, "merchant", userId);
    }

    /**
     * Creates TenantInfo for a customer user.
     */
    public static TenantInfo forCustomer(String individualId, String userId) {
        if (individualId == null || individualId.isBlank()) {
            throw new IllegalArgumentException("individualId cannot be null or blank");
        }
        return new TenantInfo(null, individualId, "customer", userId);
    }

    /**
     * Creates TenantInfo for an admin user.
     * Admins are not tied to a specific tenant.
     */
    public static TenantInfo forAdmin(String userId) {
        return new TenantInfo(null, null, "admin", userId);
    }

    public String getMerchantId() {
        return merchantId;
    }

    public String getIndividualId() {
        return individualId;
    }

    public String getRole() {
        return role;
    }

    public String getUserId() {
        return userId;
    }

    public boolean isMerchant() {
        return "merchant".equalsIgnoreCase(role);
    }

    public boolean isCustomer() {
        return "customer".equalsIgnoreCase(role);
    }

    public boolean isAdmin() {
        return "admin".equalsIgnoreCase(role);
    }

    @Override
    public String toString() {
        return "TenantInfo{" +
                "role='" + role + '\'' +
                ", merchantId='" + (merchantId != null ? merchantId : "N/A") + '\'' +
                ", individualId='" + (individualId != null ? individualId : "N/A") + '\'' +
                ", userId='" + userId + '\'' +
                '}';
    }
}
