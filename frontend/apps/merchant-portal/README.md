# Merchant Portal

Production-grade merchant dashboard for managing payments, payouts, and business operations.

## Features

### Implemented
- ✅ **Dashboard**: Revenue metrics, charts, recent transactions
- ✅ **Transactions**: List, search, filter, refund capabilities
- ✅ **Payouts**: Payout history and request functionality
- ✅ **Professional UI**: Ramp-inspired design system

### To Be Implemented
- ⏳ Disputes page
- ⏳ Settings (API keys, webhooks, profile)
- ⏳ Reports (CSV/PDF export)
- ⏳ Authentication (NextAuth.js)
- ⏳ API integration (TanStack Query)
- ⏳ Testing (Vitest + Playwright)

## Tech Stack

- **Next.js 15.1.4** - React framework with App Router
- **TypeScript 5.7** - Type safety
- **Tailwind CSS 3.4** - Styling
- **Recharts 2.15** - Data visualization
- **@fincore/ui** - Shared design system
- **@fincore/api-client** - Typed API client
- **@fincore/types** - Shared TypeScript types

## Getting Started

### Prerequisites

- Node.js >= 20
- pnpm >= 9

### Installation

```bash
# From project root
cd frontend

# Install dependencies
pnpm install

# Build shared packages
pnpm --filter @fincore/ui build
pnpm --filter @fincore/types build
pnpm --filter @fincore/api-client build
```

### Development

```bash
# Run merchant portal
pnpm --filter merchant-portal dev

# Or from root
pnpm dev  # Runs all apps
```

Visit [http://localhost:3001](http://localhost:3001)

### Build

```bash
pnpm --filter merchant-portal build
pnpm --filter merchant-portal start
```

## Project Structure

```
merchant-portal/
├── src/
│   ├── app/                    # Next.js App Router
│   │   ├── (auth)/            # Auth routes (login, signup)
│   │   ├── (portal)/          # Main app routes
│   │   │   ├── dashboard/     # Dashboard page
│   │   │   ├── transactions/  # Transactions page
│   │   │   ├── payouts/       # Payouts page
│   │   │   ├── disputes/      # (To be implemented)
│   │   │   ├── settings/      # (To be implemented)
│   │   │   └── reports/       # (To be implemented)
│   │   ├── globals.css        # Global styles
│   │   └── layout.tsx         # Root layout
│   │
│   ├── components/
│   │   ├── layout/            # Layout components
│   │   │   ├── Sidebar.tsx
│   │   │   └── Header.tsx
│   │   └── features/          # Feature components
│   │       ├── dashboard/     # Dashboard components
│   │       ├── transactions/  # Transaction components
│   │       └── payouts/       # Payout components
│   │
│   └── lib/                   # Utilities
│
├── package.json
├── next.config.js
├── tailwind.config.ts
└── tsconfig.json
```

## Environment Variables

Create `.env.local`:

```bash
# API Gateway URL
NEXT_PUBLIC_API_URL=http://localhost:8080

# NextAuth (when implemented)
NEXTAUTH_URL=http://localhost:3001
NEXTAUTH_SECRET=your-secret-here
```

## Design System

Uses Ramp-inspired design:
- **Platform Green**: #00D395 (primary actions)
- **Platform Ink**: #1C1B17 (text)
- **Warm grays**: Professional, not cold
- **Minimal**: One bold accent, monochrome base

## Next Steps

1. **API Integration**
   - Configure TanStack Query
   - Connect to backend API (localhost:8080)
   - Add loading states

2. **Authentication**
   - Configure NextAuth.js
   - Add login/signup pages
   - Protected routes

3. **Remaining Pages**
   - Disputes management
   - Settings (API keys, webhooks)
   - Reports export

4. **Testing**
   - Unit tests (Vitest)
   - E2E tests (Playwright)

5. **Polish**
   - Error handling
   - Loading skeletons
   - Optimistic updates

## API Endpoints Used

- `GET /v1/payment-intents` - List transactions
- `POST /v1/payment-intents/{id}/refund` - Refund payment
- `GET /v1/payouts` - List payouts
- `POST /v1/payouts` - Request payout
- `GET /v1/merchants/me/dashboard` - Dashboard metrics (to be added to backend)

## Contributing

This is part of the fincore monorepo. See root README for contribution guidelines.
