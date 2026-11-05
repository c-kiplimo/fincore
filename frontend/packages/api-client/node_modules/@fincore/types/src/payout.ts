/**
 * Payout Types
 * Matches backend Payout domain model
 */

export enum PayoutStatus {
  CREATED = 'CREATED',
  SENDING = 'SENDING',
  SENT = 'SENT',
  PAID = 'PAID',
  FAILED = 'FAILED',
}

export enum BeneficiaryType {
  MERCHANT = 'MERCHANT',
  WALLET = 'WALLET',
}

export enum DestinationType {
  BANK = 'BANK',
  MOBILE_MONEY = 'MOBILE_MONEY',
}

export interface Payout {
  id: string;
  entityId: string;
  beneficiaryType: BeneficiaryType;
  beneficiaryId: string;
  sourceAccount: string;
  amountMinor: number;
  currency: string;
  feeMinor: number;
  taxMinor: number;
  totalDebitedMinor: number;
  destinationType: DestinationType;
  destinationAccount: string;
  status: PayoutStatus;
  pendingTxnId?: string;
  confirmTxnId?: string;
  bankRef?: string;
  failureReason?: string;
  attempts: number;
  createdAt: string;
  updatedAt: string;
}

export interface CreatePayoutRequest {
  beneficiaryType: BeneficiaryType;
  beneficiaryId: string;
  amountMinor: number;
  currency: string;
  destinationType: DestinationType;
  destinationAccount: string;
}
