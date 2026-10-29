# Row-Level Multitenancy Implementation

This document explains how multitenancy is implemented in the FinCore using PostgreSQL Row-Level Security (RLS) and Spring Boot.

## Overview

The platform implements **row-level multitenancy** (discriminator pattern) where:
- All tenants share the same database and tables
- Each row has a tenant identifier (e.g., `merchant_id`, `individual_id`)
- PostgreSQL Row-Level Security (RLS) policies enforce data isolation
- Spring Boot components extract tenant context from JWT and set PostgreSQL session variables

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│ 1. Client Request with JWT                                  │
│    Authorization: Bearer eyJhbGc...                         │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│ 2. TenantFilter (Servlet Filter)                            │
│    - Parses JWT                                             │
│    - Extracts: userId, role, merchantId/individualId        │
│    - Sets TenantContext (ThreadLocal)                       │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│ 3. Controller (e.g., PaymentController)                     │
│    @GetMapping("/v1/payment-intents")                       │
│    - TenantContext.getCurrentMerchantId() is available      │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│ 4. Service Layer (@Transactional)                           │
│    - TenantContextAspect intercepts transaction             │
│    - Gets database connection                               │
│    - Sets PostgreSQL session variables:                     │
│      • SET LOCAL app.current_merchant_id = 'mer_123'        │
│      • SET LOCAL app.user_role = 'merchant'                 │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│ 5. PostgreSQL RLS Policies                                  │
│    SELECT * FROM payment_intents                            │
│    -- RLS Policy automatically adds:                        │
│    WHERE merchant_id = current_setting('app.current_merchant_id') │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│ 6. Result                                                    │
│    Merchant sees ONLY their payment_intents                 │
│    Admins see ALL payment_intents (bypass RLS)              │
└─────────────────────────────────────────────────────────────┘
```

## Components

### 1. Database Layer (PostgreSQL RLS)

**Migration Files:**
- `payments-service/V2__add_row_level_security.sql`
- `accounts-service/V2__add_row_level_security.sql`
- `payouts-service/V2__add_row_level_security.sql`

**Example RLS Policy:**
```sql
ALTER TABLE payment_intents ENABLE ROW LEVEL SECURITY;

-- Merchants see only their payments
CREATE POLICY merchant_isolation_payment_intents ON payment_intents
    FOR ALL
    USING (merchant_id = current_setting('app.current_merchant_id', true))
    WITH CHECK (merchant_id = current_setting('app.current_merchant_id', true));

-- Admins see all payments (bypass RLS)
CREATE POLICY admin_full_access_payment_intents ON payment_intents
    FOR ALL
    TO PUBLIC
    USING (current_setting('app.user_role', true) = 'admin');
```

**Session Variables Used:**
- `app.current_merchant_id` - Current merchant ID (for merchant users)
- `app.current_individual_id` - Current individual ID (for customer users)
- `app.user_role` - User role (admin, merchant, customer)

### 2. Spring Boot Components

#### TenantInfo
**File:** `commons/security/TenantInfo.java`

Immutable class holding tenant context:
```java
TenantInfo merchant = TenantInfo.forMerchant("mer_123", "user_456");
TenantInfo customer = TenantInfo.forCustomer("ind_789", "user_012");
TenantInfo admin = TenantInfo.forAdmin("user_999");
```

#### TenantContext
**File:** `commons/security/TenantContext.java`

ThreadLocal storage for tenant information:
```java
// Set (done by TenantFilter)
TenantContext.setTenantInfo(tenantInfo);

// Get
String merchantId = TenantContext.getCurrentMerchantId();
String role = TenantContext.getCurrentRole();
boolean isAdmin = TenantContext.isAdmin();

// Clear (MUST be done in finally block)
TenantContext.clear();
```

#### TenantFilter
**File:** `commons/filter/TenantFilter.java`

Servlet filter that extracts JWT and sets TenantContext:
```java
@Component
@Order(1) // Run before security filters
public class TenantFilter extends OncePerRequestFilter {
    // Parses JWT from Authorization header
    // Extracts: sub, role, merchantId, individualId
    // Sets TenantContext
    // Clears in finally block
}
```

**Expected JWT Claims:**
```json
{
  "sub": "user_123",
  "role": "merchant",
  "merchantId": "mer_abc123",
  "exp": 1730000000
}
```

or

```json
{
  "sub": "user_456",
  "role": "customer",
  "individualId": "ind_def456",
  "exp": 1730000000
}
```

#### TenantContextAspect
**File:** `commons/aspect/TenantContextAspect.java`

AOP aspect that sets PostgreSQL session variables before transactions:
```java
@Aspect
@Component
public class TenantContextAspect {
    // Intercepts all @Transactional methods
    // Gets database connection
    // Calls TenantConnectionCustomizer.setTenantContext()
}
```

#### TenantConnectionCustomizer
**File:** `commons/config/TenantConnectionCustomizer.java`

Sets PostgreSQL session variables on database connections:
```java
public static void setTenantContext(Connection connection) {
    TenantInfo tenant = TenantContext.getTenantInfo();

    setSessionVariable(connection, "app.user_role", tenant.getRole());
    setSessionVariable(connection, "app.current_merchant_id", tenant.getMerchantId());
    setSessionVariable(connection, "app.current_individual_id", tenant.getIndividualId());
}

private static void setSessionVariable(Connection conn, String name, String value) {
    // Uses SET LOCAL (transaction-scoped)
    conn.prepareStatement("SET LOCAL app.current_merchant_id = ?").execute();
}
```

## How It Works

### Request Flow

1. **Client sends request:**
   ```http
   GET /v1/payment-intents
   Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ1c2VyXzEyMyIsInJvbGUiOiJtZXJjaGFudCIsIm1lcmNoYW50SWQiOiJtZXJfYWJjMTIzIn0...
   ```

2. **TenantFilter parses JWT:**
   ```java
   Claims claims = Jwts.parser().parseClaimsJws(jwt).getBody();
   String userId = claims.getSubject(); // "user_123"
   String role = claims.get("role"); // "merchant"
   String merchantId = claims.get("merchantId"); // "mer_abc123"

   TenantInfo info = TenantInfo.forMerchant(merchantId, userId);
   TenantContext.setTenantInfo(info);
   ```

3. **Controller handles request:**
   ```java
   @GetMapping("/v1/payment-intents")
   public List<PaymentIntent> listPayments() {
       // TenantContext is available here
       return paymentService.getPayments();
   }
   ```

4. **Service layer opens transaction:**
   ```java
   @Transactional
   public List<PaymentIntent> getPayments() {
       // TenantContextAspect intercepts here
       // Sets: SET LOCAL app.current_merchant_id = 'mer_abc123'
       //       SET LOCAL app.user_role = 'merchant'

       return paymentRepository.findAll(); // RLS applies automatically
   }
   ```

5. **PostgreSQL executes with RLS:**
   ```sql
   -- Repository calls: SELECT * FROM payment_intents
   -- RLS policy transforms to:
   SELECT * FROM payment_intents
   WHERE merchant_id = current_setting('app.current_merchant_id', true);
   -- Returns only: WHERE merchant_id = 'mer_abc123'
   ```

6. **TenantFilter clears context:**
   ```java
   finally {
       TenantContext.clear(); // Prevent leakage to next request
   }
   ```

## Tenant Roles

### Merchant
**JWT Claims:**
```json
{ "role": "merchant", "merchantId": "mer_abc123" }
```

**Can Access:**
- Own payment_intents (WHERE merchant_id = 'mer_abc123')
- Own refunds, disputes (via payment_intents FK)
- Own reserve_holds
- Own payouts (WHERE beneficiary_type='MERCHANT' AND beneficiary_id='mer_abc123')

**Cannot Access:**
- Other merchants' data
- Customer wallet data
- Platform-wide settlement batches

### Customer
**JWT Claims:**
```json
{ "role": "customer", "individualId": "ind_def456" }
```

**Can Access:**
- Own individual record
- Own wallets
- Own transfers (from or to their wallets)
- Own payouts (WHERE beneficiary_type='WALLET' AND beneficiary_id IN their wallets)

**Cannot Access:**
- Other customers' data
- Merchant data
- Payment intents (merchants' transactions)

### Admin
**JWT Claims:**
```json
{ "role": "admin" }
```

**Can Access:**
- **EVERYTHING** - RLS policies have bypass clause:
  ```sql
  USING (current_setting('app.user_role', true) = 'admin')
  ```

**Use Cases:**
- Admin Dashboard (platform-wide analytics)
- Support operations
- Compliance investigations

## Testing Multitenancy

### Unit Tests
```java
@Test
public void testTenantIsolation() {
    // Set merchant context
    TenantInfo merchant1 = TenantInfo.forMerchant("mer_1", "user_1");
    TenantContext.setTenantInfo(merchant1);

    List<PaymentIntent> payments = paymentService.getPayments();

    // Assert: all payments belong to mer_1
    assertTrue(payments.stream().allMatch(p -> p.getMerchantId().equals("mer_1")));
}
```

### Integration Tests
```java
@Test
@Sql("/tenant-isolation-test-data.sql")
public void testRlsPolicies() {
    // Insert 2 merchants, each with payments
    // Set context to merchant 1
    TenantInfo merchant1 = TenantInfo.forMerchant("mer_1", "user_1");
    TenantContext.setTenantInfo(merchant1);

    // Query should only return merchant 1's payments
    List<PaymentIntent> payments = jdbcTemplate.query(
        "SELECT * FROM payment_intents",
        (rs, rowNum) -> mapPayment(rs)
    );

    assertEquals(5, payments.size()); // Only merchant 1's 5 payments
    assertTrue(payments.stream().allMatch(p -> p.getMerchantId().equals("mer_1")));
}
```

### E2E Tests
```bash
# Login as merchant 1
curl -X POST http://localhost:8080/v1/auth/login \
  -d '{"email":"merchant1@example.com","password":"pass"}' \
  -H "Content-Type: application/json"
# Returns JWT with merchantId=mer_1

# Get payments (should see only mer_1's payments)
curl http://localhost:8080/v1/payment-intents \
  -H "Authorization: Bearer <JWT>"

# Try to access another merchant's payment (should fail/return 404)
curl http://localhost:8080/v1/payment-intents/pi_belongs_to_mer_2 \
  -H "Authorization: Bearer <JWT>"
# RLS blocks this - returns empty or 404
```

## Security Considerations

### ✅ Strengths

1. **Database-Level Enforcement:** RLS is enforced in PostgreSQL, not application code
   - Even if application code has bugs, database blocks unauthorized access
   - Cannot be bypassed with SQL injection

2. **Defense in Depth:** Multiple layers:
   - JWT validation (API Gateway)
   - Application-level checks (if any)
   - Database RLS policies

3. **Indexed Filtering:** RLS uses existing indexes (merchant_id, individual_id)
   - No performance degradation

4. **Admin Escape Hatch:** Admins can bypass RLS for support/compliance
   - Controlled via `app.user_role = 'admin'`

### ⚠️ Potential Issues

1. **Session Variable Not Set:**
   - If TenantContextAspect fails to set variables, queries return empty results
   - **Mitigation:** Logging, monitoring, fail-closed design

2. **Thread Leakage:**
   - If TenantContext.clear() is not called, next request might inherit tenant
   - **Mitigation:** Always clear in finally block (TenantFilter ensures this)

3. **Admin Privilege Escalation:**
   - If JWT is compromised and role changed to admin, user gets full access
   - **Mitigation:** JWT validation in API Gateway, short expiry, secret rotation

4. **Cross-Service Tenant Validation:**
   - payouts-service might need to validate beneficiary belongs to caller
   - **Mitigation:** Additional application-level checks for cross-service calls

## Configuration

### Enable RLS (Per Service)

**application.yml:**
```yaml
# JWT secret for parsing tokens (should be same across all services)
jwt:
  secret: ${JWT_SECRET:changeme-secret-key-for-development-only}

# Enable AOP for TenantContextAspect
spring:
  aop:
    auto: true
```

### Enable Flyway Migrations

Ensure migrations run on startup:
```yaml
spring:
  flyway:
    enabled: true
    locations: classpath:db/migration
    validate-on-migrate: true
```

### Logging (for debugging)

```yaml
logging:
  level:
    com.collins.platform.commons.filter.TenantFilter: DEBUG
    com.collins.platform.commons.aspect.TenantContextAspect: DEBUG
```

## Troubleshooting

### Issue: Queries return empty results for authenticated users

**Cause:** Session variables not set correctly.

**Debug:**
```sql
-- In PostgreSQL, check session variables
SELECT current_setting('app.current_merchant_id', true);
SELECT current_setting('app.user_role', true);
```

**Fix:** Check that TenantContextAspect is running (enable DEBUG logging).

### Issue: User sees other tenants' data

**Cause:** RLS not enabled or policy misconfigured.

**Debug:**
```sql
-- Check if RLS is enabled
SELECT tablename, rowsecurity FROM pg_tables WHERE tablename = 'payment_intents';
-- rowsecurity should be 't' (true)

-- Check policies
SELECT * FROM pg_policies WHERE tablename = 'payment_intents';
```

**Fix:** Run V2 migration scripts.

### Issue: Admin cannot see all data

**Cause:** Admin policy missing or app.user_role not set to 'admin'.

**Debug:**
```sql
-- Check admin policy exists
SELECT * FROM pg_policies WHERE policyname LIKE '%admin%';
```

**Fix:** Ensure JWT has `"role": "admin"` and policy exists.

## References

- [PostgreSQL Row-Level Security Docs](https://www.postgresql.org/docs/current/ddl-rowsecurity.html)
- [Spring AOP Documentation](https://docs.spring.io/spring-framework/reference/core/aop.html)
- [ThreadLocal Best Practices](https://www.baeldung.com/java-threadlocal)
