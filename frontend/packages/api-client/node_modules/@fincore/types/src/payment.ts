/**
 * Payment Intent Types
 * Matches backend PaymentIntent domain model
 */

export enum PaymentStatus {
  AUTHORIZED = 'AUTHORIZED',
  CAPTURED = 'CAPTURED',
  PARTIALLY_REFUNDED = 'PARTIALLY_REFUNDED',
  REFUNDED = 'REFUNDED',
  DISPUTED = 'DISPUTED',
  CHARGED_BACK = 'CHARGED_BACK',
  CANCELED = 'CANCELED',
  DECLINED = 'DECLINED',
}

export enum PaymentMethod {
  CARD = 'CARD',
  MOBILE_MONEY = 'MOBILE_MONEY',
}

export interface PaymentIntent {
  id: string;
  entityId: string;
  merchantId: string;
  amountMinor: number;
  currency: string;
  status: PaymentStatus;
  method: PaymentMethod;
  networkRef?: string;
  declineReason?: string;
  capturedMinor?: number;
  refundedMinor?: number;
  feeMinor?: number;
  taxMinor?: number;
  netMinor?: number;
  quoteId?: string;
  description?: string;
  createdAt: string;
  updatedAt: string;
}

export interface CreatePaymentRequest {
  merchantId: string;
  amountMinor: number;
  currency: string;
  method: PaymentMethod;
  instrument: {
    cardNumber?: string;
    expiryMonth?: number;
    expiryYear?: number;
    cvv?: string;
    phoneNumber?: string;
  };
  description?: string;
}

export interface RefundRequest {
  amountMinor?: number; // Partial refund if specified
}

export interface Dispute {
  id: string;
  paymentId: string;
  entityId: string;
  merchantId: string;
  amountMinor: number;
  feeMinor: number;
  currency: string;
  reason: string;
  status: DisputeStatus;
  resolvedAt?: string;
  createdAt: string;
}

export enum DisputeStatus {
  OPEN = 'OPEN',
  WON = 'WON',
  LOST = 'LOST',
}
