import type { AxiosInstance } from 'axios';
import type {
  UserProfile,
  UpdateProfileRequest,
  ApiKey,
  CreateApiKeyRequest,
  CreateApiKeyResponse,
  WebhookConfig,
  UpdateWebhookRequest,
  WebhookEvent,
} from '@fincore/types';

/**
 * Settings API Client
 */
export class SettingsApi {
  constructor(private client: AxiosInstance) {}

  // Profile
  async getProfile(): Promise<UserProfile> {
    const response = await this.client.get<UserProfile>('/v1/auth/me');
    return response.data;
  }

  async updateProfile(data: UpdateProfileRequest): Promise<UserProfile> {
    const response = await this.client.put<UserProfile>('/v1/profile', { payload: data });
    return response.data;
  }

  // API Keys
  async listApiKeys(): Promise<ApiKey[]> {
    const response = await this.client.get<ApiKey[]>('/v1/api-keys');
    return response.data;
  }

  async createApiKey(data: CreateApiKeyRequest): Promise<CreateApiKeyResponse> {
    const response = await this.client.post<CreateApiKeyResponse>('/v1/api-keys', { payload: data });
    return response.data;
  }

  async revokeApiKey(keyId: string): Promise<void> {
    await this.client.delete(`/v1/api-keys/${keyId}`);
  }

  // Webhooks
  async getWebhookConfig(): Promise<WebhookConfig> {
    const response = await this.client.get<WebhookConfig>('/v1/webhooks/config');
    return response.data;
  }

  async updateWebhookConfig(data: UpdateWebhookRequest): Promise<WebhookConfig> {
    const response = await this.client.put<WebhookConfig>('/v1/webhooks/config', { payload: data });
    return response.data;
  }

  async testWebhook(): Promise<void> {
    await this.client.post('/v1/webhooks/test');
  }

  async listWebhookEvents(params?: { page?: number; pageSize?: number }): Promise<WebhookEvent[]> {
    const response = await this.client.get<WebhookEvent[]>('/v1/webhooks/events', { params });
    return response.data;
  }
}
