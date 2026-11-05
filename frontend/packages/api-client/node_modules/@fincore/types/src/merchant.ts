/**
 * Merchant & Wallet Types
 * Matches backend Account domain models
 */

export interface Merchant {
  id: string;
  entityId: string;
  name: string;
  email: string;
  businessType: string;
  country: string;
  currency: string;
  ledgerAccount: string;
  status: MerchantStatus;
  createdAt: string;
  updatedAt: string;
}

export enum MerchantStatus {
  ACTIVE = 'ACTIVE',
  SUSPENDED = 'SUSPENDED',
  CLOSED = 'CLOSED',
}

export interface Wallet {
  id: string;
  entityId: string;
  individualId: string;
  currency: string;
  ledgerAccount: string;
  status: WalletStatus;
  createdAt: string;
}

export enum WalletStatus {
  ACTIVE = 'ACTIVE',
  FROZEN = 'FROZEN',
  CLOSED = 'CLOSED',
}

export interface MerchantBalance {
  merchantId: string;
  currency: string;
  availableMinor: number;
  pendingMinor: number;
  reserveMinor: number;
}

export interface WalletBalance {
  walletId: string;
  currency: string;
  balanceMinor: number;
}
