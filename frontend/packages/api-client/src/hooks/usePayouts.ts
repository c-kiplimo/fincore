import { useQuery, useMutation, useQueryClient, type UseQueryOptions, type UseMutationOptions } from '@tanstack/react-query';
import type { Payout, CreatePayoutRequest } from '@fincore/types';
import type { PayoutsApi } from '../payouts';

/**
 * React Query hooks for Payouts API
 */

export const payoutKeys = {
  all: ['payouts'] as const,
  lists: () => [...payoutKeys.all, 'list'] as const,
  list: (params?: any) => [...payoutKeys.lists(), params] as const,
  details: () => [...payoutKeys.all, 'detail'] as const,
  detail: (id: string) => [...payoutKeys.details(), id] as const,
};

/**
 * Hook to fetch a single payout by ID
 */
export function usePayout(
  api: PayoutsApi,
  id: string,
  options?: Omit<UseQueryOptions<Payout>, 'queryKey' | 'queryFn'>
) {
  return useQuery({
    queryKey: payoutKeys.detail(id),
    queryFn: () => api.getPayout(id),
    ...options,
  });
}

/**
 * Hook to fetch list of payouts
 */
export function usePayouts(
  api: PayoutsApi,
  params?: any,
  options?: Omit<UseQueryOptions<any>, 'queryKey' | 'queryFn'>
) {
  return useQuery({
    queryKey: payoutKeys.list(params),
    queryFn: () => api.listPayouts(params),
    ...options,
  });
}

/**
 * Hook to create a payout
 */
export function useCreatePayout(
  api: PayoutsApi,
  options?: UseMutationOptions<Payout, Error, CreatePayoutRequest>
) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (data: CreatePayoutRequest) => api.createPayout(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: payoutKeys.lists() });
    },
    ...options,
  });
}

/**
 * Hook to retry a failed payout
 */
export function useRetryPayout(
  api: PayoutsApi,
  options?: UseMutationOptions<Payout, Error, string>
) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (id: string) => api.retryPayout(id),
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: payoutKeys.detail(data.id) });
      queryClient.invalidateQueries({ queryKey: payoutKeys.lists() });
    },
    ...options,
  });
}
