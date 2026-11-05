import type { AxiosInstance } from 'axios';
import type {
  Wallet,
  Individual,
  Transfer,
  CreateWalletRequest,
  TopUpRequest,
  TransferRequest,
  ConversionRequest,
  WalletPaymentRequest,
  RegisterIndividualRequest,
} from '@fincore/types';
import { withIdempotency } from './client';

/**
 * Wallets API Client
 */
export class WalletsApi {
  constructor(private client: AxiosInstance) {}

  // Individuals
  async registerIndividual(data: RegisterIndividualRequest): Promise<Individual> {
    const response = await this.client.post<Individual>('/v1/individuals', { payload: data });
    return response.data;
  }

  async getIndividual(individualId: string): Promise<Individual> {
    const response = await this.client.get<Individual>(`/v1/individuals/${individualId}`);
    return response.data;
  }

  // Wallets
  async openWallet(individualId: string, data: CreateWalletRequest): Promise<Wallet> {
    const response = await this.client.post<Wallet>(
      `/v1/individuals/${individualId}/wallets`,
      { payload: data }
    );
    return response.data;
  }

  async getWallet(walletId: string): Promise<Wallet> {
    const response = await this.client.get<Wallet>(`/v1/wallets/${walletId}`);
    return response.data;
  }

  async listWallets(individualId: string): Promise<Wallet[]> {
    const response = await this.client.get<Wallet[]>(`/v1/individuals/${individualId}/wallets`);
    return response.data;
  }

  async getTransferHistory(walletId: string): Promise<Transfer[]> {
    const response = await this.client.get<Transfer[]>(`/v1/wallets/${walletId}/transfers`);
    return response.data;
  }

  // Operations
  async topUp(walletId: string, data: TopUpRequest): Promise<Transfer> {
    const response = await this.client.post<Transfer>(
      `/v1/wallets/${walletId}/top-ups`,
      { payload: data },
      withIdempotency()
    );
    return response.data;
  }

  async transfer(data: TransferRequest): Promise<Transfer> {
    const response = await this.client.post<Transfer>(
      '/v1/transfers',
      { payload: data },
      withIdempotency()
    );
    return response.data;
  }

  async convert(data: ConversionRequest): Promise<Transfer> {
    const response = await this.client.post<Transfer>(
      '/v1/conversions',
      { payload: data },
      withIdempotency()
    );
    return response.data;
  }

  async payMerchant(data: WalletPaymentRequest): Promise<Transfer> {
    const response = await this.client.post<Transfer>(
      '/v1/wallet-payments',
      { payload: data },
      withIdempotency()
    );
    return response.data;
  }
}
