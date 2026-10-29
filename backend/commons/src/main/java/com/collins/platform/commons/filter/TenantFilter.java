package com.collins.platform.commons.filter;

import com.collins.platform.commons.security.TenantContext;
import com.collins.platform.commons.security.TenantInfo;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Servlet filter that extracts tenant information from JWT and sets TenantContext.
 *
 * This filter runs BEFORE security filters and extracts:
 * - User ID (JWT subject)
 * - Role (merchant, customer, admin)
 * - Merchant ID (if role=merchant)
 * - Individual ID (if role=customer)
 *
 * The extracted information is stored in ThreadLocal via {@link TenantContext}
 * and used by RLS policies and application-level filtering.
 *
 * JWT Claims Expected:
 * - sub: user ID
 * - role: merchant | customer | admin
 * - merchantId: merchant ID (only if role=merchant)
 * - individualId: individual ID (only if role=customer)
 */
@Component
@Order(1) // Run before security filters
public class TenantFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(TenantFilter.class);

    @Value("${jwt.secret:changeme-secret-key-for-development-only}")
    private String jwtSecret;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            // Extract JWT from Authorization header
            String jwt = extractJwt(request);

            if (jwt != null) {
                // Parse JWT and extract claims
                Claims claims = parseJwt(jwt);

                if (claims != null) {
                    String userId = claims.getSubject();
                    String role = claims.get("role", String.class);
                    String merchantId = claims.get("merchantId", String.class);
                    String individualId = claims.get("individualId", String.class);

                    // Create TenantInfo based on role
                    TenantInfo tenantInfo = createTenantInfo(userId, role, merchantId, individualId);

                    if (tenantInfo != null) {
                        TenantContext.setTenantInfo(tenantInfo);
                        log.debug("Set tenant context: {}", tenantInfo);
                    }
                }
            }

            // Continue filter chain
            filterChain.doFilter(request, response);

        } finally {
            // CRITICAL: Always clear context to prevent leakage between requests
            TenantContext.clear();
        }
    }

    /**
     * Extracts JWT from Authorization header.
     * Expects format: "Bearer <token>"
     */
    private String extractJwt(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7); // Remove "Bearer " prefix
        }

        return null;
    }

    /**
     * Parses JWT and extracts claims.
     * Returns null if JWT is invalid or expired.
     */
    private Claims parseJwt(String jwt) {
        try {
            return Jwts.parser()
                    .setSigningKey(jwtSecret.getBytes())
                    .parseClaimsJws(jwt)
                    .getBody();
        } catch (Exception e) {
            log.warn("Failed to parse JWT: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Creates TenantInfo from JWT claims.
     */
    private TenantInfo createTenantInfo(String userId, String role, String merchantId, String individualId) {
        if (role == null) {
            log.warn("JWT missing role claim, user: {}", userId);
            return null;
        }

        switch (role.toLowerCase()) {
            case "merchant":
                if (merchantId == null) {
                    log.warn("Merchant user missing merchantId claim, user: {}", userId);
                    return null;
                }
                return TenantInfo.forMerchant(merchantId, userId);

            case "customer":
                if (individualId == null) {
                    log.warn("Customer user missing individualId claim, user: {}", userId);
                    return null;
                }
                return TenantInfo.forCustomer(individualId, userId);

            case "admin":
                return TenantInfo.forAdmin(userId);

            default:
                log.warn("Unknown role in JWT: {}, user: {}", role, userId);
                return null;
        }
    }
}
