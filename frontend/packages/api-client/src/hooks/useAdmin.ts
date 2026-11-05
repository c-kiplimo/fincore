import { useQuery, useMutation, useQueryClient, type UseQueryOptions, type UseMutationOptions } from '@tanstack/react-query';
import type { PlatformMetrics, SystemHealth, MerchantListItem, SettlementBatch } from '@fincore/types';
import type { AdminApi, ListMerchantsParams, ListSettlementsParams } from '../admin';

export const adminKeys = {
  metrics: (startDate?: string, endDate?: string) => ['admin', 'metrics', startDate, endDate] as const,
  health: ['admin', 'health'] as const,
  merchants: (params?: ListMerchantsParams) => ['admin', 'merchants', params] as const,
  settlements: (params?: ListSettlementsParams) => ['admin', 'settlements', params] as const,
};

export function usePlatformMetrics(
  api: AdminApi,
  startDate?: string,
  endDate?: string,
  options?: Omit<UseQueryOptions<PlatformMetrics>, 'queryKey' | 'queryFn'>
) {
  return useQuery({
    queryKey: adminKeys.metrics(startDate, endDate),
    queryFn: () => api.getPlatformMetrics(startDate, endDate),
    ...options,
  });
}

export function useSystemHealth(
  api: AdminApi,
  options?: Omit<UseQueryOptions<SystemHealth>, 'queryKey' | 'queryFn'>
) {
  return useQuery({
    queryKey: adminKeys.health,
    queryFn: () => api.getSystemHealth(),
    refetchInterval: 30000, // Refresh every 30 seconds
    ...options,
  });
}

export function useAdminMerchants(
  api: AdminApi,
  params?: ListMerchantsParams,
  options?: Omit<UseQueryOptions<MerchantListItem[]>, 'queryKey' | 'queryFn'>
) {
  return useQuery({
    queryKey: adminKeys.merchants(params),
    queryFn: () => api.listMerchants(params || {}),
    ...options,
  });
}

export function useSuspendMerchant(
  api: AdminApi,
  options?: UseMutationOptions<void, Error, { merchantId: string; reason: string }>
) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ merchantId, reason }) => api.suspendMerchant(merchantId, reason),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'merchants'] });
    },
    ...options,
  });
}

export function useActivateMerchant(
  api: AdminApi,
  options?: UseMutationOptions<void, Error, string>
) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (merchantId: string) => api.activateMerchant(merchantId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'merchants'] });
    },
    ...options,
  });
}

export function useSettlements(
  api: AdminApi,
  params?: ListSettlementsParams,
  options?: Omit<UseQueryOptions<SettlementBatch[]>, 'queryKey' | 'queryFn'>
) {
  return useQuery({
    queryKey: adminKeys.settlements(params),
    queryFn: () => api.listSettlements(params || {}),
    ...options,
  });
}

export function useTriggerSettlement(
  api: AdminApi,
  options?: UseMutationOptions<SettlementBatch, Error, string>
) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (entityId: string) => api.triggerSettlement(entityId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'settlements'] });
    },
    ...options,
  });
}

export function useReleaseDueReserves(
  api: AdminApi,
  options?: UseMutationOptions<{ released: number }, Error, string | undefined>
) {
  return useMutation({
    mutationFn: (asOf?: string) => api.releaseDueReserves(asOf),
    ...options,
  });
}
