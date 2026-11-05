'use client';

import {
  createApiClient,
  PaymentsApi,
  PayoutsApi,
  MerchantsApi,
  DisputesApi,
  ReportsApi,
  SettingsApi,
  AdminApi,
} from '@fincore/api-client';
import { getSession } from 'next-auth/react';

const API_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080';

// Create API client with auth token from session
const client = createApiClient({
  baseURL: API_URL,
  getAccessToken: async () => {
    const session = await getSession();
    return session?.accessToken ?? null;
  },
  onUnauthorized: () => {
    // Redirect to login on 401
    window.location.href = '/login';
  },
});

// Export singleton API instances
export const paymentsApi = new PaymentsApi(client);
export const payoutsApi = new PayoutsApi(client);
export const merchantsApi = new MerchantsApi(client);
export const disputesApi = new DisputesApi(client);
export const reportsApi = new ReportsApi(client);
export const settingsApi = new SettingsApi(client);
export const adminApi = new AdminApi(client);
