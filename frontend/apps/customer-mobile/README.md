# Customer Mobile App - Payments Platform

React Native mobile application for the Payments Platform customer wallet.

## Features

- **Wallet Management**: View balance, recent transactions, quick actions
- **Payments**: QR code scanning, manual payments, merchant history
- **Transfers**: P2P transfers, bank withdrawals
- **Profile**: Security settings, linked accounts, saved cards, preferences

## Tech Stack

- **React Native**: 0.76.6
- **TypeScript**: 5.7.2
- **React Navigation**: Bottom tabs + stack navigation
- **TanStack Query**: Data fetching and caching
- **Zustand**: Lightweight state management

## Project Structure

```
src/
├── features/           # Feature-based modules
│   ├── auth/          # Authentication (login, signup)
│   ├── wallet/        # Wallet features
│   ├── payments/      # Payment features
│   ├── transfers/     # Transfer features
│   └── profile/       # Profile features
├── navigation/        # Navigation configuration
├── components/        # Shared UI components
├── theme/             # Design system (colors, spacing, typography)
├── services/          # API clients
├── hooks/             # Custom React hooks
├── utils/             # Utility functions
└── App.tsx            # Root component
```

## Getting Started

### Prerequisites

- Node.js >= 18
- React Native CLI
- Xcode (for iOS development)
- Android Studio (for Android development)

### Installation

```bash
# Install dependencies
npm install

# iOS: Install pods
cd ios && pod install && cd ..
```

### Running the App

```bash
# Start Metro bundler
npm start

# Run on iOS
npm run ios

# Run on Android
npm run android
```

## Design System

The app uses the Ramp-inspired design system matching the web applications:

- **Primary Color**: Platform Green (#00D395)
- **Typography**: Inter (sans), IBM Plex Mono (mono)
- **Spacing**: 4px base unit
- **Theme**: Minimal, monochrome with bold accent

## Development

### Type Checking

```bash
npm run type-check
```

### Linting

```bash
npm run lint
```

### Testing

```bash
npm test
```

## Platform-Specific Notes

### iOS

- Minimum iOS version: 13.0
- Supports iPhone and iPad
- Uses Face ID/Touch ID for biometric authentication

### Android

- Minimum Android version: 6.0 (API 23)
- Supports fingerprint authentication

## API Integration

The app connects to the backend API at `http://localhost:8080` (configurable).

API client is shared via TanStack Query with:
- Automatic retries (2 attempts)
- 5-minute stale time
- Query invalidation on mutations

## Next Steps

- [ ] Add authentication (JWT storage, biometric login)
- [ ] Connect to backend API
- [ ] Implement QR code scanning (react-native-camera)
- [ ] Add push notifications (react-native-push-notification)
- [ ] Implement offline support (AsyncStorage caching)
- [ ] Add E2E tests (Maestro)
- [ ] Add animations (React Native Reanimated)

## License

Private
