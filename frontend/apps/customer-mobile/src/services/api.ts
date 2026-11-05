import AsyncStorage from '@react-native-async-storage/async-storage';
import {
  createApiClient,
  PaymentsApi,
  WalletsApi,
  SettingsApi,
} from '@fincore/api-client';

const API_URL = process.env.EXPO_PUBLIC_API_URL || 'http://localhost:8080';

// Create API client with auth token from AsyncStorage
const client = createApiClient({
  baseURL: API_URL,
  getAccessToken: async () => {
    try {
      const token = await AsyncStorage.getItem('accessToken');
      return token ?? null;
    } catch (error) {
      console.error('Failed to get access token:', error);
      return null;
    }
  },
  onUnauthorized: async () => {
    // Clear stored token and redirect to login
    try {
      await AsyncStorage.removeItem('accessToken');
      await AsyncStorage.removeItem('refreshToken');
      // Navigation will be handled by the auth state
    } catch (error) {
      console.error('Failed to clear tokens:', error);
    }
  },
});

// Export singleton API instances
export const paymentsApi = new PaymentsApi(client);
export const walletsApi = new WalletsApi(client);
export const settingsApi = new SettingsApi(client);

// Helper functions for token management
export const setAuthTokens = async (accessToken: string, refreshToken?: string) => {
  try {
    await AsyncStorage.setItem('accessToken', accessToken);
    if (refreshToken) {
      await AsyncStorage.setItem('refreshToken', refreshToken);
    }
  } catch (error) {
    console.error('Failed to store auth tokens:', error);
  }
};

export const clearAuthTokens = async () => {
  try {
    await AsyncStorage.removeItem('accessToken');
    await AsyncStorage.removeItem('refreshToken');
  } catch (error) {
    console.error('Failed to clear auth tokens:', error);
  }
};

export const getStoredAccessToken = async (): Promise<string | null> => {
  try {
    return await AsyncStorage.getItem('accessToken');
  } catch (error) {
    console.error('Failed to get stored access token:', error);
    return null;
  }
};
