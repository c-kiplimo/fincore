import type { AxiosInstance } from 'axios';
import type { Merchant, PaginatedResponse } from '@fincore/types';

export interface OnboardMerchantRequest {
  entityId: string;
  name: string;
  country: string;
  settlementCurrency: string;
  reservePercentBp: number;
  reserveDays: number;
}

export interface MerchantBalances {
  merchantId: string;
  currency: string;
  payableMinor: number;
  reserveMinor: number;
  availableForPayoutMinor: number;
}

export interface ListMerchantsParams {
  entityId?: string;
  page?: number;
  pageSize?: number;
}

/**
 * Merchants API Client
 */
export class MerchantsApi {
  constructor(private client: AxiosInstance) {}

  async onboardMerchant(data: OnboardMerchantRequest): Promise<Merchant> {
    const response = await this.client.post<Merchant>('/v1/merchants', { payload: data });
    return response.data;
  }

  async getMerchant(merchantId: string): Promise<Merchant> {
    const response = await this.client.get<Merchant>(`/v1/merchants/${merchantId}`);
    return response.data;
  }

  async listMerchants(params: ListMerchantsParams): Promise<Merchant[]> {
    const response = await this.client.get<Merchant[]>('/v1/merchants', { params });
    return response.data;
  }

  async getMerchantBalances(merchantId: string): Promise<MerchantBalances> {
    const response = await this.client.get<MerchantBalances>(`/v1/merchants/${merchantId}/balances`);
    return response.data;
  }

  async getReserves(merchantId: string): Promise<any[]> {
    const response = await this.client.get<any[]>(`/v1/merchants/${merchantId}/reserves`);
    return response.data;
  }
}
