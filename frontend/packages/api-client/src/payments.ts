import type { AxiosInstance } from 'axios';
import type {
  PaymentIntent,
  CreatePaymentRequest,
  RefundRequest,
  Dispute,
  DisputeStatus,
  PaginatedResponse,
} from '@fincore/types';
import { withIdempotency } from './client';

export interface ListPaymentsParams {
  page?: number;
  pageSize?: number;
  status?: string;
  merchantId?: string;
  startDate?: string;
  endDate?: string;
}

/**
 * Payments API Client
 * All payment-related endpoints
 */
export class PaymentsApi {
  constructor(private client: AxiosInstance) {}

  /**
   * Create a new payment intent (authorization)
   */
  async createPaymentIntent(data: CreatePaymentRequest): Promise<PaymentIntent> {
    const response = await this.client.post<PaymentIntent>(
      '/v1/payment-intents',
      data,
      withIdempotency()
    );
    return response.data;
  }

  /**
   * Get payment intent by ID
   */
  async getPaymentIntent(id: string): Promise<PaymentIntent> {
    const response = await this.client.get<PaymentIntent>(
      `/v1/payment-intents/${id}`
    );
    return response.data;
  }

  /**
   * List payment intents with pagination
   */
  async listPaymentIntents(
    params?: ListPaymentsParams
  ): Promise<PaginatedResponse<PaymentIntent>> {
    const response = await this.client.get<PaginatedResponse<PaymentIntent>>(
      '/v1/payment-intents',
      { params }
    );
    return response.data;
  }

  /**
   * Capture an authorized payment
   */
  async capturePayment(id: string): Promise<PaymentIntent> {
    const response = await this.client.post<PaymentIntent>(
      `/v1/payment-intents/${id}/capture`,
      {},
      withIdempotency()
    );
    return response.data;
  }

  /**
   * Cancel an authorized payment
   */
  async cancelPayment(id: string): Promise<PaymentIntent> {
    const response = await this.client.post<PaymentIntent>(
      `/v1/payment-intents/${id}/cancel`,
      {},
      withIdempotency()
    );
    return response.data;
  }

  /**
   * Refund a captured payment (full or partial)
   */
  async refundPayment(
    id: string,
    data?: RefundRequest
  ): Promise<PaymentIntent> {
    const response = await this.client.post<PaymentIntent>(
      `/v1/payment-intents/${id}/refund`,
      data || {},
      withIdempotency()
    );
    return response.data;
  }

  /**
   * Open a dispute/chargeback for a payment
   */
  async openDispute(
    paymentId: string,
    reason: string
  ): Promise<Dispute> {
    const response = await this.client.post<Dispute>(
      `/v1/payment-intents/${paymentId}/disputes`,
      { reason },
      withIdempotency()
    );
    return response.data;
  }

  /**
   * Resolve a dispute (won/lost)
   */
  async resolveDispute(
    paymentId: string,
    disputeId: string,
    outcome: DisputeStatus.WON | DisputeStatus.LOST
  ): Promise<Dispute> {
    const response = await this.client.post<Dispute>(
      `/v1/payment-intents/${paymentId}/disputes/${disputeId}/resolve`,
      { outcome },
      withIdempotency()
    );
    return response.data;
  }

  /**
   * List all disputes
   */
  async listDisputes(params?: {
    page?: number;
    pageSize?: number;
    status?: string;
  }): Promise<PaginatedResponse<Dispute>> {
    const response = await this.client.get<PaginatedResponse<Dispute>>(
      '/v1/disputes',
      { params }
    );
    return response.data;
  }
}
