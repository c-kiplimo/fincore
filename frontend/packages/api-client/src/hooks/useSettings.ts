import { useQuery, useMutation, useQueryClient, type UseQueryOptions, type UseMutationOptions } from '@tanstack/react-query';
import type { UserProfile, UpdateProfileRequest, ApiKey, CreateApiKeyRequest, CreateApiKeyResponse, WebhookConfig, UpdateWebhookRequest, WebhookEvent } from '@fincore/types';
import type { SettingsApi } from '../settings';

export const settingsKeys = {
  profile: ['settings', 'profile'] as const,
  apiKeys: ['settings', 'api-keys'] as const,
  webhook: ['settings', 'webhook'] as const,
  webhookEvents: (params?: any) => ['settings', 'webhook-events', params] as const,
};

export function useProfile(
  api: SettingsApi,
  options?: Omit<UseQueryOptions<UserProfile>, 'queryKey' | 'queryFn'>
) {
  return useQuery({
    queryKey: settingsKeys.profile,
    queryFn: () => api.getProfile(),
    ...options,
  });
}

export function useUpdateProfile(
  api: SettingsApi,
  options?: UseMutationOptions<UserProfile, Error, UpdateProfileRequest>
) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (data: UpdateProfileRequest) => api.updateProfile(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: settingsKeys.profile });
    },
    ...options,
  });
}

export function useApiKeys(
  api: SettingsApi,
  options?: Omit<UseQueryOptions<ApiKey[]>, 'queryKey' | 'queryFn'>
) {
  return useQuery({
    queryKey: settingsKeys.apiKeys,
    queryFn: () => api.listApiKeys(),
    ...options,
  });
}

export function useCreateApiKey(
  api: SettingsApi,
  options?: UseMutationOptions<CreateApiKeyResponse, Error, CreateApiKeyRequest>
) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (data: CreateApiKeyRequest) => api.createApiKey(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: settingsKeys.apiKeys });
    },
    ...options,
  });
}

export function useRevokeApiKey(
  api: SettingsApi,
  options?: UseMutationOptions<void, Error, string>
) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (keyId: string) => api.revokeApiKey(keyId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: settingsKeys.apiKeys });
    },
    ...options,
  });
}

export function useWebhookConfig(
  api: SettingsApi,
  options?: Omit<UseQueryOptions<WebhookConfig>, 'queryKey' | 'queryFn'>
) {
  return useQuery({
    queryKey: settingsKeys.webhook,
    queryFn: () => api.getWebhookConfig(),
    ...options,
  });
}

export function useUpdateWebhookConfig(
  api: SettingsApi,
  options?: UseMutationOptions<WebhookConfig, Error, UpdateWebhookRequest>
) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (data: UpdateWebhookRequest) => api.updateWebhookConfig(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: settingsKeys.webhook });
    },
    ...options,
  });
}

export function useTestWebhook(
  api: SettingsApi,
  options?: UseMutationOptions<void, Error, void>
) {
  return useMutation({
    mutationFn: () => api.testWebhook(),
    ...options,
  });
}

export function useWebhookEvents(
  api: SettingsApi,
  params?: { page?: number; pageSize?: number },
  options?: Omit<UseQueryOptions<WebhookEvent[]>, 'queryKey' | 'queryFn'>
) {
  return useQuery({
    queryKey: settingsKeys.webhookEvents(params),
    queryFn: () => api.listWebhookEvents(params),
    ...options,
  });
}
