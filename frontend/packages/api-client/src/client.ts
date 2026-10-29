import axios, { type AxiosInstance, type AxiosRequestConfig } from 'axios';
import { v4 as uuidv4 } from 'uuid';

/**
 * Base API Client Configuration
 */
export interface ApiClientConfig {
  baseURL: string;
  getAccessToken?: () => Promise<string | null>;
  onUnauthorized?: () => void;
}

/**
 * Creates a configured Axios instance for API communication
 */
export function createApiClient(config: ApiClientConfig): AxiosInstance {
  const { baseURL, getAccessToken, onUnauthorized } = config;

  const client = axios.create({
    baseURL,
    headers: {
      'Content-Type': 'application/json',
    },
    timeout: 30000, // 30 seconds
  });

  // Request interceptor: Add JWT token
  client.interceptors.request.use(
    async (requestConfig) => {
      if (getAccessToken) {
        const token = await getAccessToken();
        if (token) {
          requestConfig.headers.Authorization = `Bearer ${token}`;
        }
      }
      return requestConfig;
    },
    (error) => Promise.reject(error)
  );

  // Response interceptor: Handle errors
  client.interceptors.response.use(
    (response) => response,
    (error) => {
      if (error.response?.status === 401 && onUnauthorized) {
        onUnauthorized();
      }
      return Promise.reject(error);
    }
  );

  return client;
}

/**
 * Generates a unique idempotency key for money-moving operations
 */
export function generateIdempotencyKey(): string {
  return uuidv4();
}

/**
 * Helper function to add idempotency key to request config
 */
export function withIdempotency(
  config?: AxiosRequestConfig
): AxiosRequestConfig {
  return {
    ...config,
    headers: {
      ...config?.headers,
      'Idempotency-Key': generateIdempotencyKey(),
    },
  };
}
