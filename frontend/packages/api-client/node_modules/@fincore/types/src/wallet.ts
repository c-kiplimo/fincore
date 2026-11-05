/**
 * Wallet and transfer types
 */

export interface Wallet {
  id: string;
  individualId: string;
  entityId: string;
  currency: string;
  kind: 'MAIN' | 'SAVINGS';
  ledgerAccount: string;
  status: 'ACTIVE' | 'SUSPENDED' | 'CLOSED';
  balanceMinor: number;
  createdAt: string;
}

export interface Individual {
  id: string;
  entityId: string;
  name: string;
  country: string;
  createdAt: string;
}

export interface Transfer {
  id: string;
  transferType: string;
  entityId: string;
  fromRef?: string;
  toRef?: string;
  amountMinor: number;
  currency: string;
  feeMinor: number;
  taxMinor: number;
  targetAmountMinor?: number;
  targetCurrency?: string;
  quoteId?: string;
  ledgerTxnId?: string;
  createdAt: string;
}

export interface CreateWalletRequest {
  currency: string;
  kind: 'MAIN' | 'SAVINGS';
}

export interface TopUpRequest {
  amountMinor: number;
  method: string;
}

export interface TransferRequest {
  fromWalletId: string;
  toWalletId: string;
  amountMinor: number;
}

export interface ConversionRequest {
  fromWalletId: string;
  toWalletId: string;
  amountMinor: number;
}

export interface WalletPaymentRequest {
  walletId: string;
  merchantId: string;
  amountMinor: number;
}

export interface RegisterIndividualRequest {
  entityId: string;
  name: string;
  country: string;
  mainCurrency: string;
}
