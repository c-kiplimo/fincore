import type { AxiosInstance } from 'axios';
import type {
  Payout,
  CreatePayoutRequest,
  PaginatedResponse,
} from '@payments-platform/types';
import { withIdempotency } from './client';

export interface ListPayoutsParams {
  page?: number;
  pageSize?: number;
  status?: string;
  beneficiaryId?: string;
}

/**
 * Payouts API Client
 * All payout-related endpoints
 */
export class PayoutsApi {
  constructor(private client: AxiosInstance) {}

  /**
   * Create a new payout
   */
  async createPayout(data: CreatePayoutRequest): Promise<Payout> {
    const response = await this.client.post<Payout>(
      '/v1/payouts',
      data,
      withIdempotency()
    );
    return response.data;
  }

  /**
   * Get payout by ID
   */
  async getPayout(id: string): Promise<Payout> {
    const response = await this.client.get<Payout>(`/v1/payouts/${id}`);
    return response.data;
  }

  /**
   * List payouts with pagination
   */
  async listPayouts(
    params?: ListPayoutsParams
  ): Promise<PaginatedResponse<Payout>> {
    const response = await this.client.get<PaginatedResponse<Payout>>(
      '/v1/payouts',
      { params }
    );
    return response.data;
  }

  /**
   * Retry a failed payout
   */
  async retryPayout(id: string): Promise<Payout> {
    const response = await this.client.post<Payout>(
      `/v1/payouts/${id}/retry`,
      {},
      withIdempotency()
    );
    return response.data;
  }
}
