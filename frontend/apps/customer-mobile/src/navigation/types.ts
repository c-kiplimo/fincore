/**
 * Navigation type definitions for the app.
 * Provides type-safe navigation throughout the application.
 */

import { NavigatorScreenParams } from '@react-navigation/native';

// Auth Stack
export type AuthStackParamList = {
  Login: undefined;
  Signup: undefined;
};

// Wallet Stack
export type WalletStackParamList = {
  WalletHome: undefined;
  TopUp: undefined;
  TransactionDetail: { transactionId: string };
};

// Payments Stack
export type PaymentsStackParamList = {
  PaymentHome: undefined;
  QRScan: undefined;
  PaymentConfirm: {
    merchantId: string;
    amount: number;
  };
  PaymentSuccess: {
    transactionId: string;
  };
};

// Transfers Stack
export type TransfersStackParamList = {
  TransferHome: undefined;
  SendMoney: undefined;
  Withdraw: undefined;
  TransferConfirm: {
    recipient: string;
    amount: number;
  };
};

// Profile Stack
export type ProfileStackParamList = {
  ProfileHome: undefined;
  Security: undefined;
  LinkedAccounts: undefined;
  SavedCards: undefined;
  Notifications: undefined;
  Help: undefined;
};

// Bottom Tab Navigator
export type TabNavigatorParamList = {
  WalletTab: NavigatorScreenParams<WalletStackParamList>;
  PaymentsTab: NavigatorScreenParams<PaymentsStackParamList>;
  TransfersTab: NavigatorScreenParams<TransfersStackParamList>;
  ProfileTab: NavigatorScreenParams<ProfileStackParamList>;
};

// Root Navigator
export type RootStackParamList = {
  Auth: NavigatorScreenParams<AuthStackParamList>;
  App: NavigatorScreenParams<TabNavigatorParamList>;
};

// Helper types for navigation
declare global {
  namespace ReactNavigation {
    interface RootParamList extends RootStackParamList {}
  }
}
