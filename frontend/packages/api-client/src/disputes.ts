import type { AxiosInstance } from 'axios';
import type { Dispute, CreateDisputeRequest, ResolveDisputeRequest } from '@fincore/types';
import { withIdempotency } from './client';

export interface ListDisputesParams {
  page?: number;
  pageSize?: number;
  status?: string;
  merchantId?: string;
}

/**
 * Disputes API Client
 */
export class DisputesApi {
  constructor(private client: AxiosInstance) {}

  async openDispute(paymentIntentId: string, data: CreateDisputeRequest): Promise<Dispute> {
    const response = await this.client.post<Dispute>(
      `/v1/payment-intents/${paymentIntentId}/disputes`,
      { payload: data },
      withIdempotency()
    );
    return response.data;
  }

  async getDispute(disputeId: string): Promise<Dispute> {
    const response = await this.client.get<Dispute>(`/v1/disputes/${disputeId}`);
    return response.data;
  }

  async listDisputes(params: ListDisputesParams): Promise<Dispute[]> {
    const response = await this.client.get<Dispute[]>('/v1/disputes', { params });
    return response.data;
  }

  async resolveDispute(disputeId: string, data: ResolveDisputeRequest): Promise<Dispute> {
    const response = await this.client.post<Dispute>(
      `/v1/disputes/${disputeId}/resolve`,
      { payload: data }
    );
    return response.data;
  }

  async uploadEvidence(disputeId: string, file: File, description?: string): Promise<Dispute> {
    const formData = new FormData();
    formData.append('file', file);
    if (description) {
      formData.append('description', description);
    }

    const response = await this.client.post<Dispute>(
      `/v1/disputes/${disputeId}/evidence`,
      formData,
      {
        headers: {
          'Content-Type': 'multipart/form-data',
        },
      }
    );
    return response.data;
  }
}
