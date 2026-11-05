/**
 * Settings and configuration types
 */

export interface UserProfile {
  id: string;
  email: string;
  name: string;
  role: 'ADMIN' | 'MERCHANT' | 'CUSTOMER';
  tenantId: string;
  merchantId?: string;
  individualId?: string;
  createdAt: string;
  updatedAt?: string;
}

export interface UpdateProfileRequest {
  name?: string;
  email?: string;
  phone?: string;
  timezone?: string;
  notifications?: NotificationPreferences;
}

export interface NotificationPreferences {
  emailNotifications: boolean;
  smsNotifications: boolean;
  pushNotifications: boolean;
  transactionAlerts: boolean;
  payoutAlerts: boolean;
  disputeAlerts: boolean;
}

export interface ApiKey {
  id: string;
  name: string;
  keyPrefix: string;
  status: 'ACTIVE' | 'REVOKED';
  lastUsedAt?: string;
  expiresAt?: string;
  createdAt: string;
}

export interface CreateApiKeyRequest {
  name: string;
  expiresInDays?: number;
}

export interface CreateApiKeyResponse {
  apiKey: ApiKey;
  secretKey: string; // Only returned once
}

export interface WebhookConfig {
  id: string;
  url: string;
  events: string[];
  status: 'ACTIVE' | 'INACTIVE';
  secret: string;
  lastDeliveryAt?: string;
  createdAt: string;
  updatedAt?: string;
}

export interface UpdateWebhookRequest {
  url?: string;
  events?: string[];
  status?: 'ACTIVE' | 'INACTIVE';
}

export interface WebhookEvent {
  id: string;
  type: string;
  url: string;
  payload: any;
  responseStatus?: number;
  responseBody?: string;
  deliveredAt?: string;
  failureReason?: string;
  createdAt: string;
}
