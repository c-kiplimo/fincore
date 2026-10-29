# Quick Start Guide - FinCore

**Complete end-to-end payments platform with 11 backend microservices and 4 frontend applications.**

---

## 🚀 Access All UIs

### Web Applications (Next.js)

| Application | URL | Port | Demo Login |
|-------------|-----|------|------------|
| **Admin Dashboard** | http://localhost:3000 | 3000 | `admin@fincore.com` / `admin123` |
| **Merchant Portal** | http://localhost:3001 | 3001 | `merchant@example.com` / `password123` |
| **Customer Web** | http://localhost:3002 | 3002 | (Pending backend auth) |

### Mobile Application (React Native)

| Application | Platform | Access |
|-------------|----------|--------|
| **Customer Mobile** | iOS/Android | Emulator or Physical Device |

---

## 📱 How to Launch Apps

### 1. Admin Dashboard (Port 3000)

```bash
cd frontend
pnpm install                      # First time only
pnpm --filter admin-dashboard dev
```

**Then visit**: http://localhost:3000

**Login**: `admin@fincore.com` / `admin123`

---

### 2. Merchant Portal (Port 3001)

```bash
cd frontend
pnpm install                      # First time only
pnpm --filter merchant-portal dev
```

**Then visit**: http://localhost:3001

**Login**: `merchant@example.com` / `password123`

---

### 3. Customer Web (Port 3002)

```bash
cd frontend
pnpm install                      # First time only
pnpm --filter customer-web dev
```

**Then visit**: http://localhost:3002

---

### 4. Customer Mobile App

#### On Android Emulator

```bash
cd frontend/apps/customer-mobile
npm install                       # First time only
npm start                         # Start Metro bundler
# In new terminal:
npm run android
```

#### On Your Android Phone

**📱 See complete guide**: [frontend/apps/customer-mobile/ANDROID_DEPLOYMENT.md](./frontend/apps/customer-mobile/ANDROID_DEPLOYMENT.md)

**Quick Steps**:
1. Enable Developer Mode on phone (Settings → About → Tap "Build number" 7 times)
2. Enable USB Debugging (Settings → Developer options → USB debugging)
3. Connect phone via USB cable
4. Run:
   ```bash
   cd frontend/apps/customer-mobile
   npm install
   npm run android
   ```

**Alternative - Install APK**:
```bash
cd frontend/apps/customer-mobile/android
./gradlew assembleDebug
# APK is at: app/build/outputs/apk/debug/app-debug.apk
# Transfer to phone and install
```

#### On iOS (macOS only)

```bash
cd frontend/apps/customer-mobile
npm install                       # First time only
cd ios && pod install && cd ..    # First time only
npm run ios
```

---

## 🔧 Environment Setup

### First Time Setup

**1. Copy environment files**:
```bash
# Admin Dashboard
cd frontend/apps/admin-dashboard
cp .env.example .env.local

# Merchant Portal
cd ../merchant-portal
cp .env.example .env.local

# Customer Web
cd ../customer-web
cp .env.example .env.local
```

**2. Generate NextAuth secret**:
```bash
openssl rand -base64 32
```

**3. Edit each `.env.local` file**:
```env
NEXT_PUBLIC_API_URL=http://localhost:8080
NEXTAUTH_URL=http://localhost:3000  # Change port per app
NEXTAUTH_SECRET=<paste-generated-secret>
```

---

## 🖥️ Backend Services

### Start All Backend Services

**Option 1: Docker (Recommended)**
```bash
docker-compose up -d
```

**Option 2: Individual Services**
```bash
cd backend/api-gateway
mvn spring-boot:run
```

**Verify Backend**:
- API Gateway: http://localhost:8080/actuator/health
- All services listed in `IMPLEMENTATION_GUIDE.md`

---

## 📊 What Each UI Does

### Admin Dashboard (Port 3000)
**Who uses it**: Platform administrators

**Features**:
- View all platform metrics (revenue, transactions)
- Manage merchants (activate, suspend, configure fees)
- Monitor all transactions across platform
- Resolve disputes
- Trigger settlements
- Monitor system health (microservices, Kafka)
- Generate reports

**Access**: Admin role required

---

### Merchant Portal (Port 3001)
**Who uses it**: Business owners accepting payments

**Features**:
- View revenue dashboard with charts
- Manage transactions (view, search, refund)
- Request payouts to bank account
- Handle disputes (upload evidence)
- Configure API keys and webhooks
- Download transaction reports

**Access**: Merchant role required

---

### Customer Web (Port 3002)
**Who uses it**: End customers (web version)

**Features**:
- View wallet balance
- Make payments to merchants
- Send money to other users (P2P)
- Withdraw to bank account
- Manage profile and security settings

**Access**: Customer role required

---

### Customer Mobile App
**Who uses it**: End customers (mobile version)

**Features**:
- Wallet balance and transactions
- QR code scanning to pay merchants
- P2P transfers
- Bank withdrawals
- Profile and security settings

**Platforms**: iOS and Android
**Same features** as Customer Web, optimized for mobile

---

## 🎨 Design System

All UIs use **Ramp-inspired design system**:

- **Primary Color**: Platform Green `#00D395`
- **Typography**: Inter (sans-serif)
- **Style**: Minimal, monochrome with bold accent
- **Theme**: Professional fintech look

---

## 🔐 Authentication

### Current Status

**Web Apps**: NextAuth.js configured with JWT
- Admin: `admin@fincore.com` / `admin123`
- Merchant: `merchant@example.com` / `password123`
- Customer: Pending backend endpoint

**Mobile App**: Ready for authentication integration

**Note**: Backend `/v1/auth/login` endpoint not yet implemented. Web apps use mock authentication in development.

---

## 📖 Documentation

| Document | Purpose |
|----------|---------|
| [IMPLEMENTATION_GUIDE.md](./IMPLEMENTATION_GUIDE.md) | Complete architecture and implementation details (600+ lines) |
| [README_MULTITENANCY.md](./backend/commons/src/main/java/com/collins/platform/commons/security/README_MULTITENANCY.md) | Row-level multitenancy explanation (400+ lines) |
| [COMPLETION_SUMMARY.md](./COMPLETION_SUMMARY.md) | What was built, statistics, achievements |
| [VERIFICATION_REPORT.md](./VERIFICATION_REPORT.md) | End-to-end verification and testing |
| [ANDROID_DEPLOYMENT.md](./frontend/apps/customer-mobile/ANDROID_DEPLOYMENT.md) | Deploy Android app to physical device |

---

## 🧪 Testing

### Run Frontend Tests

```bash
cd frontend/apps/merchant-portal
pnpm test              # Vitest unit tests
pnpm test:e2e          # Playwright E2E tests
```

### Run Backend Tests

```bash
cd backend/payments-service
mvn test               # JUnit unit tests
mvn verify             # Integration tests
```

---

## 🔍 Troubleshooting

### Frontend Issues

**"Module not found" errors**:
```bash
cd frontend
pnpm install
```

**Port already in use**:
```bash
# Change port in package.json scripts:
# "dev": "next dev -p 3003"
```

**NextAuth errors**:
- Make sure `.env.local` exists with `NEXTAUTH_SECRET`
- Generate secret with: `openssl rand -base64 32`

### Mobile App Issues

**Android: "adb: device not found"**:
```bash
adb kill-server
adb start-server
adb devices
```

**Metro bundler not starting**:
```bash
cd frontend/apps/customer-mobile
npx react-native start --reset-cache
```

**Build errors**:
```bash
cd android
./gradlew clean
cd ..
npm run android
```

**See full troubleshooting**: [ANDROID_DEPLOYMENT.md](./frontend/apps/customer-mobile/ANDROID_DEPLOYMENT.md)

---

## 🎯 Common Tasks

### Start All Web Apps (3 terminals)

**Terminal 1**:
```bash
cd frontend && pnpm --filter admin-dashboard dev
```

**Terminal 2**:
```bash
cd frontend && pnpm --filter merchant-portal dev
```

**Terminal 3**:
```bash
cd frontend && pnpm --filter customer-web dev
```

Then visit:
- Admin: http://localhost:3000
- Merchant: http://localhost:3001
- Customer: http://localhost:3002

---

### Install Mobile App on Your Phone

**Full guide**: [ANDROID_DEPLOYMENT.md](./frontend/apps/customer-mobile/ANDROID_DEPLOYMENT.md)

**Quick method**:
```bash
# 1. Enable USB debugging on phone
# 2. Connect phone via USB
# 3. Run:
cd frontend/apps/customer-mobile
npm run android
```

---

### Build Production APK

```bash
cd frontend/apps/customer-mobile/android
./gradlew assembleRelease
# APK: app/build/outputs/apk/release/app-release.apk
```

---

## 📦 Project Structure

```
fincore/
├── backend/                    # 11 Spring Boot microservices
│   ├── api-gateway/           # Port 8080 (entry point)
│   ├── ledger-service/        # Port 8081
│   ├── payments-service/      # Port 8083
│   ├── accounts-service/      # Port 8084
│   ├── payouts-service/       # Port 8085
│   └── ... (6 more services)
│
├── frontend/
│   ├── packages/              # Shared monorepo packages
│   │   ├── ui/                # Design system
│   │   ├── types/             # TypeScript types
│   │   └── api-client/        # API client + hooks
│   │
│   └── apps/
│       ├── admin-dashboard/   # Next.js (3000)
│       ├── merchant-portal/   # Next.js (3001)
│       ├── customer-web/      # Next.js (3002)
│       └── customer-mobile/   # React Native
│
└── docs/
```

---

## 🎉 Next Steps

After launching the UIs:

1. ✅ **Explore all pages** in each UI
2. ✅ **Test authentication** flows
3. ✅ **Try mobile app** on your phone
4. 📖 **Read documentation** for deeper understanding
5. 🧪 **Run tests** to verify everything works
6. 🚀 **Deploy to production** (see IMPLEMENTATION_GUIDE.md)

---

## 📞 Support

**Documentation**:
- Main Guide: [IMPLEMENTATION_GUIDE.md](./IMPLEMENTATION_GUIDE.md)
- Multitenancy: [README_MULTITENANCY.md](./backend/commons/src/main/java/com/collins/platform/commons/security/README_MULTITENANCY.md)
- Android Deployment: [ANDROID_DEPLOYMENT.md](./frontend/apps/customer-mobile/ANDROID_DEPLOYMENT.md)

**Technology Docs**:
- Next.js: https://nextjs.org/docs
- React Native: https://reactnative.dev/docs
- Spring Boot: https://spring.io/guides

---

**Happy Coding!** 🚀

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>
