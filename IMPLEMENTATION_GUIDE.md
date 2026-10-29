# FinCore - Complete Implementation Guide

## Table of Contents

1. [Overview](#overview)
2. [Architecture](#architecture)
3. [Getting Started](#getting-started)
4. [Backend Services](#backend-services)
5. [Frontend Applications](#frontend-applications)
6. [Authentication & Authorization](#authentication--authorization)
7. [Row-Level Multitenancy](#row-level-multitenancy)
8. [API Integration](#api-integration)
9. [Testing](#testing)
10. [Deployment](#deployment)
11. [Development Workflow](#development-workflow)

---

## Overview

The FinCore is a production-grade, end-to-end fintech solution with:

- **Backend**: 11 Spring Boot microservices with event-driven architecture
- **Frontend**: 3 professional UIs (Admin Dashboard, Merchant Portal, Customer App)
- **Design**: Ramp-inspired design system - minimal, professional, human-designed
- **Security**: Row-level multitenancy with PostgreSQL RLS
- **Testing**: Full E2E coverage from UI to database

### Key Features

✅ **Payment Processing**: Card payments, mobile money, authorizations, captures, refunds
✅ **Payouts**: Merchant settlements with saga pattern for reliability
✅ **Lending**: Credit line management and loan processing
✅ **Wallets**: Customer wallets with P2P transfers
✅ **Double-Entry Ledger**: Accurate financial records via gRPC
✅ **Dispute Management**: Chargeback handling with evidence upload
✅ **Multi-Currency**: Support for KES, USD, and more
✅ **Rate Limiting**: Redis-based rate limiting per merchant
✅ **Multitenancy**: Database-level isolation with RLS

---

## Architecture

### System Diagram

```
┌──────────────────────────────────────────────────────────────────┐
│                         Clients                                   │
├─────────────┬────────────────┬──────────────┬────────────────────┤
│ Admin       │ Merchant       │ Customer Web │ Customer Mobile    │
│ Dashboard   │ Portal         │ (Next.js)    │ (React Native)     │
│ (Next.js)   │ (Next.js)      │ Port 3002    │ iOS/Android        │
│ Port 3000   │ Port 3001      │              │                    │
└──────┬──────┴────────┬───────┴──────┬───────┴──────┬─────────────┘
       │               │              │              │
       └───────────────┴──────────────┴──────────────┘
                         │
                    JWT Auth (NextAuth.js / Biometric)
                         │
       ┌─────────────────┴─────────────────┐
       │    API Gateway (Port 8080)        │
       │    - JWT Validation               │
       │    - Rate Limiting (Redis)        │
       │    - Routing                      │
       └─────────────────┬─────────────────┘
                         │
       ┌─────────────────┴─────────────────┐
       │         Spring Boot Services      │
       ├───────────────────────────────────┤
       │ • Ledger Service (8081)           │
       │ • Pricing Service (8082)          │
       │ • Payments Service (8083)         │
       │ • Accounts Service (8084)         │
       │ • Payouts Service (8085)          │
       │ • Lending Service (8086)          │
       │ • Reconciliation (8087)           │
       │ • Rails Simulator (8088)          │
       └─────────┬─────────────────────────┘
                 │
       ┌─────────┴─────────────────────────┐
       │  Infrastructure                   │
       ├───────────────────────────────────┤
       │ • PostgreSQL (per service)        │
       │ • Kafka (event streaming)         │
       │ • Redis (rate limiting, cache)    │
       │ • gRPC (ledger communication)     │
       └───────────────────────────────────┘
```

### Technology Stack

**Backend:**
- Java 21, Spring Boot 3.4.0
- PostgreSQL 15 with Row-Level Security
- Apache Kafka for event streaming
- Redis for rate limiting
- gRPC for ledger API
- Flyway for database migrations
- Transactional Outbox pattern

**Frontend:**
- Next.js 15.1.4 (App Router)
- React 18.3.1, TypeScript 5.7.2
- React Native 0.76.6 (mobile)
- TailwindCSS 3.4.17
- NextAuth.js 5.0 (authentication)
- TanStack Query 5.x (data fetching)
- Zustand 5.x (state management)

**Design System:**
- Ramp-inspired theme
- Primary Color: Platform Green (#00D395)
- Typography: Inter (sans), IBM Plex Mono (mono)
- Minimal, monochrome with one bold accent

---

## Getting Started

### Prerequisites

- **Java**: JDK 21 or higher
- **Node.js**: 18+ (for frontend)
- **Docker**: Latest version
- **Maven**: 3.9+
- **pnpm**: 8+ (for monorepo)

### Quick Start

```bash
# 1. Clone the repository
git clone <repository-url>
cd fincore

# 2. Start infrastructure (PostgreSQL, Kafka, Redis)
docker-compose up -d

# 3. Build and run backend services
mvn clean install
cd backend/api-gateway && mvn spring-boot:run

# 4. Install frontend dependencies
cd frontend
pnpm install

# 5. Run frontend apps
pnpm --filter merchant-portal dev  # Port 3001
pnpm --filter admin-dashboard dev  # Port 3000
pnpm --filter customer-web dev     # Port 3002
```

### Environment Variables

Create `.env.local` in each frontend app:

```bash
# Backend API URL
NEXT_PUBLIC_API_URL=http://localhost:8080

# NextAuth secret (generate with: openssl rand -base64 32)
NEXTAUTH_SECRET=your-secret-key-here

# NextAuth URL
NEXTAUTH_URL=http://localhost:3001
```

---

## Backend Services

### Service Overview

| Service | Port | Purpose |
|---------|------|---------|
| API Gateway | 8080 | Entry point, auth, rate limiting |
| Ledger Service | 8081 | Double-entry bookkeeping via gRPC |
| Pricing Service | 8082 | Fee/tax calculation |
| Payments Service | 8083 | Payment processing, disputes |
| Accounts Service | 8084 | Merchants, wallets, customers |
| Payouts Service | 8085 | Merchant payouts (saga pattern) |
| Lending Service | 8086 | Credit lines, loans |
| Reconciliation | 8087 | Bank reconciliation |
| Rails Simulator | 8088 | Mock bank/card network |

### Feature-Based Structure

Each service follows a feature-based (not layer-based) structure:

```
payments-service/
├── api/                    # HTTP controllers
│   ├── PaymentIntentController.java
│   └── DisputeController.java
├── features/               # Feature modules
│   ├── payment/
│   │   ├── PaymentService.java
│   │   ├── PaymentRepository.java
│   │   └── PaymentIntent.java
│   └── dispute/
│       ├── DisputeService.java
│       └── Dispute.java
├── infrastructure/         # External integrations
│   ├── SimulatorClient.java
│   └── LedgerClient.java
└── config/                 # Configuration
    └── DatabaseConfig.java
```

### Running Individual Services

```bash
# Run all services
./scripts/start-all-services.sh

# Run specific service
cd backend/payments-service
mvn spring-boot:run

# With profile
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### Database Migrations

Migrations run automatically on startup (Flyway):

```bash
# Check migration status
cd backend/payments-service
mvn flyway:info

# Manually run migrations
mvn flyway:migrate
```

---

## Frontend Applications

### 1. Admin Dashboard (Port 3000)

**Purpose**: Platform administration and monitoring

**Pages**:
- Dashboard: Platform metrics, revenue charts
- Merchants: Manage all merchants
- Transactions: Platform-wide transaction monitoring
- Disputes: All disputes with resolution
- Settlements: Network settlement batches
- System Health: Microservices status, Kafka monitoring
- Reports: Analytics, CSV/PDF export

**Access**:
- Role: `admin`
- Demo: admin@fincore.com / admin123

### 2. Merchant Portal (Port 3001)

**Purpose**: Merchant business management

**Pages**:
- Dashboard: Revenue metrics with charts
- Transactions: View, search, refund
- Payouts: Payout history and requests
- Disputes: Handle chargebacks
- Settings: API keys, webhooks, profile
- Reports: Transaction, revenue, fee reports

**Access**:
- Role: `merchant`
- Demo: merchant@example.com / password123

### 3. Customer Web App (Port 3002)

**Purpose**: Customer wallet management (web)

**Pages**:
- Wallet: Balance, transactions, top-up
- Payments: QR scanner, merchant payments
- Transfers: P2P, bank withdrawals
- Profile: Security, linked accounts, settings

**Access**:
- Role: `customer`
- Demo: (pending backend auth endpoint)

### 4. Customer Mobile App (React Native)

**Purpose**: Customer wallet management (mobile)

**Features**:
- Same as web app
- Biometric authentication (Face ID, Touch ID)
- QR code scanning (payments)
- Push notifications
- Offline support

**Running**:

```bash
cd frontend/apps/customer-mobile

# iOS
npm run ios

# Android
npm run android
```

---

## Authentication & Authorization

### NextAuth.js (Web Apps)

**Flow**:
1. User submits email/password to `/login`
2. NextAuth calls `POST /v1/auth/login` (backend)
3. Backend validates credentials and returns JWT
4. NextAuth stores JWT in session (httpOnly cookie)
5. Session includes: `{ id, role, merchantId/individualId, accessToken }`

**Middleware Protection**:

```typescript
// middleware.ts
export default auth((req) => {
  if (!req.auth && !isPublicRoute) {
    return NextResponse.redirect('/login');
  }
});
```

**Using Session**:

```typescript
import { auth } from '@/lib/auth';

const session = await auth(); // Server component
// OR
import { useSession } from 'next-auth/react';
const { data: session } = useSession(); // Client component
```

### JWT Claims

**Merchant**:
```json
{
  "sub": "user_123",
  "role": "merchant",
  "merchantId": "mer_abc123",
  "exp": 1730000000
}
```

**Admin**:
```json
{
  "sub": "user_456",
  "role": "admin",
  "exp": 1730000000
}
```

**Customer**:
```json
{
  "sub": "user_789",
  "role": "customer",
  "individualId": "ind_def456",
  "exp": 1730000000
}
```

---

## Row-Level Multitenancy

### Database-Level Isolation

PostgreSQL Row-Level Security (RLS) enforces data isolation:

```sql
-- Example: payment_intents table
ALTER TABLE payment_intents ENABLE ROW LEVEL SECURITY;

-- Merchants see only their payments
CREATE POLICY merchant_isolation_payment_intents ON payment_intents
    FOR ALL
    USING (merchant_id = current_setting('app.current_merchant_id', true));

-- Admins see all payments
CREATE POLICY admin_full_access_payment_intents ON payment_intents
    FOR ALL
    TO PUBLIC
    USING (current_setting('app.user_role', true) = 'admin');
```

### Spring Boot Integration

**TenantFilter** extracts JWT and sets ThreadLocal:

```java
@Component
@Order(1)
public class TenantFilter extends OncePerRequestFilter {
    protected void doFilterInternal(...) {
        // Parse JWT from Authorization header
        Claims claims = parseJwt(jwt);

        // Create TenantInfo
        TenantInfo info = TenantInfo.forMerchant(
            claims.get("merchantId"),
            claims.getSubject()
        );

        // Set in ThreadLocal
        TenantContext.setTenantInfo(info);
    }
}
```

**TenantContextAspect** sets PostgreSQL session variables:

```java
@Aspect
public class TenantContextAspect {
    @Around("@annotation(Transactional)")
    public Object setTenantContext(ProceedingJoinPoint joinPoint) {
        Connection conn = DataSourceUtils.getConnection(dataSource);

        // SET LOCAL app.current_merchant_id = 'mer_123'
        // SET LOCAL app.user_role = 'merchant'
        TenantConnectionCustomizer.setTenantContext(conn);

        return joinPoint.proceed();
    }
}
```

**Result**: Queries automatically filter by tenant.

See full documentation: [backend/commons/src/main/java/com/collins/platform/commons/security/README_MULTITENANCY.md](backend/commons/src/main/java/com/collins/platform/commons/security/README_MULTITENANCY.md)

---

## API Integration

### TanStack Query Hooks

**Using payments API**:

```typescript
import { usePayments, useRefundPayment } from '@fincore/api-client/hooks';
import { paymentsApi } from '@/lib/api';

function TransactionsPage() {
  // Fetch payments
  const { data, isLoading } = usePayments(paymentsApi, {
    page: 0,
    pageSize: 20,
    status: 'CAPTURED',
  });

  // Refund mutation
  const refund = useRefundPayment(paymentsApi, {
    onSuccess: () => {
      alert('Refund successful!');
    },
  });

  const handleRefund = (id: string) => {
    refund.mutate({ id, data: { amountMinor: 5000 } });
  };

  return <div>...</div>;
}
```

### API Client Setup

```typescript
// lib/api.ts
import { createApiClient, PaymentsApi } from '@fincore/api-client';

const client = createApiClient({
  baseURL: process.env.NEXT_PUBLIC_API_URL!,
  getAccessToken: async () => {
    const session = await getSession();
    return session?.accessToken || null;
  },
  onUnauthorized: () => {
    signOut({ callbackUrl: '/login' });
  },
});

export const paymentsApi = new PaymentsApi(client);
```

---

## Testing

### Testing Pyramid

```
      /\
     /  \     10% E2E (Playwright/Maestro)
    /    \
   /------\   20% Integration (React Testing Library / Spring Boot Test)
  /        \
 /----------\ 70% Unit (Vitest / JUnit)
```

### Frontend Testing

**Vitest (Unit Tests)**:

```bash
cd frontend/apps/merchant-portal
pnpm test
```

**Playwright (E2E Tests)**:

```bash
pnpm test:e2e
```

**Example E2E Test**:

```typescript
test('merchant can refund payment', async ({ page }) => {
  await page.goto('http://localhost:3001');
  await page.fill('input[name=email]', 'merchant@test.com');
  await page.fill('input[name=password]', 'password123');
  await page.click('button[type=submit]');

  await page.goto('/transactions');
  await page.click('tr:first-child >> text=Refund');
  await page.fill('input[name=amount]', '5000');
  await page.click('text=Confirm');

  await expect(page.locator('text=Refund successful')).toBeVisible();
});
```

### Backend Testing

**JUnit (Unit Tests)**:

```bash
cd backend/payments-service
mvn test
```

**Integration Tests with Testcontainers**:

```java
@SpringBootTest
@Testcontainers
class PaymentIntegrationTest {
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15");

    @Test
    void endToEndPaymentFlow() {
        PaymentIntent payment = paymentService.create(/* ... */);
        PaymentIntent captured = paymentService.capture(payment.getId());

        assertEquals(PaymentStatus.CAPTURED, captured.getStatus());
    }
}
```

---

## Deployment

### Docker Deployment

```bash
# Build all services
docker-compose build

# Start all services
docker-compose up -d

# Check status
docker-compose ps

# View logs
docker-compose logs -f payments-service
```

### Production Checklist

**Backend**:
- [ ] Set `SPRING_PROFILES_ACTIVE=prod`
- [ ] Configure external PostgreSQL (not Docker)
- [ ] Set up Kafka cluster (3+ brokers)
- [ ] Configure Redis cluster
- [ ] Set secure JWT secret
- [ ] Enable SSL/TLS for all services
- [ ] Configure log aggregation (ELK stack)
- [ ] Set up monitoring (Prometheus + Grafana)
- [ ] Configure backups (database, Kafka)

**Frontend**:
- [ ] Set `NODE_ENV=production`
- [ ] Build apps: `pnpm build`
- [ ] Set secure `NEXTAUTH_SECRET`
- [ ] Configure CDN for static assets
- [ ] Enable rate limiting at edge (Cloudflare)
- [ ] Set up error tracking (Sentry)
- [ ] Configure analytics (PostHog, Mixpanel)

---

## Development Workflow

### Git Workflow

```bash
# Create feature branch
git checkout -b feature/my-feature

# Make changes and commit
git add .
git commit -m "feat(payments): add refund functionality"

# Push and create PR
git push origin feature/my-feature
gh pr create --title "Add refund functionality" --body "..."
```

### Commit Conventions

Use Conventional Commits:

```
feat(scope): add new feature
fix(scope): fix bug
refactor(scope): code refactoring
test(scope): add tests
docs(scope): update documentation
chore(scope): tooling changes
```

### Code Review Checklist

- [ ] All tests pass
- [ ] No security vulnerabilities
- [ ] Follows project structure conventions
- [ ] Proper error handling
- [ ] Transaction isolation (multitenancy)
- [ ] Idempotency for money operations
- [ ] Documentation updated

---

## Additional Resources

- **Multitenancy Guide**: [backend/commons/.../README_MULTITENANCY.md](backend/commons/src/main/java/com/collins/platform/commons/security/README_MULTITENANCY.md)
- **API Documentation**: http://localhost:8080/swagger-ui.html (when services running)
- **Mobile App Guide**: [frontend/apps/customer-mobile/README.md](frontend/apps/customer-mobile/README.md)

---

## Support

For issues or questions:
- GitHub Issues: <repository-url>/issues
- Email: support@fincore.com

---

**Built with best practices from 2026 industry standards.**
Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>
