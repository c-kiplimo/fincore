import type { AxiosInstance } from 'axios';
import type {
  PlatformMetrics,
  SystemHealth,
  MerchantListItem,
  SettlementBatch,
} from '@fincore/types';

export interface ListMerchantsParams {
  page?: number;
  pageSize?: number;
  status?: string;
  country?: string;
}

export interface ListSettlementsParams {
  page?: number;
  pageSize?: number;
  entityId?: string;
  status?: string;
}

/**
 * Admin API Client
 */
export class AdminApi {
  constructor(private client: AxiosInstance) {}

  // Metrics & Dashboard
  async getPlatformMetrics(startDate?: string, endDate?: string): Promise<PlatformMetrics> {
    const response = await this.client.get<PlatformMetrics>('/v1/admin/metrics', {
      params: { startDate, endDate },
    });
    return response.data;
  }

  async getSystemHealth(): Promise<SystemHealth> {
    const response = await this.client.get<SystemHealth>('/v1/admin/health');
    return response.data;
  }

  // Merchants Management
  async listMerchants(params: ListMerchantsParams): Promise<MerchantListItem[]> {
    const response = await this.client.get<MerchantListItem[]>('/v1/admin/merchants', { params });
    return response.data;
  }

  async suspendMerchant(merchantId: string, reason: string): Promise<void> {
    await this.client.post(`/v1/admin/merchants/${merchantId}/suspend`, { payload: { reason } });
  }

  async activateMerchant(merchantId: string): Promise<void> {
    await this.client.post(`/v1/admin/merchants/${merchantId}/activate`);
  }

  // Settlements
  async listSettlements(params: ListSettlementsParams): Promise<SettlementBatch[]> {
    const response = await this.client.get<SettlementBatch[]>('/v1/settlements', { params });
    return response.data;
  }

  async triggerSettlement(entityId: string): Promise<SettlementBatch> {
    const response = await this.client.post<SettlementBatch>('/v1/settlements', {
      payload: { entityId },
    });
    return response.data;
  }

  // System Operations
  async releaseDueReserves(asOf?: string): Promise<{ released: number }> {
    const response = await this.client.post<{ released: number }>('/v1/reserves/release-due', {
      params: { asOf },
    });
    return response.data;
  }
}
