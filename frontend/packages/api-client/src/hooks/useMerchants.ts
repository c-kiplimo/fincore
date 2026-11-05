import { useQuery, useMutation, useQueryClient, type UseQueryOptions, type UseMutationOptions } from '@tanstack/react-query';
import type { Merchant } from '@fincore/types';
import type { MerchantsApi, OnboardMerchantRequest, ListMerchantsParams, MerchantBalances } from '../merchants';

export const merchantKeys = {
  all: ['merchants'] as const,
  lists: () => [...merchantKeys.all, 'list'] as const,
  list: (params?: ListMerchantsParams) => [...merchantKeys.lists(), params] as const,
  details: () => [...merchantKeys.all, 'detail'] as const,
  detail: (id: string) => [...merchantKeys.details(), id] as const,
  balances: (id: string) => [...merchantKeys.detail(id), 'balances'] as const,
  reserves: (id: string) => [...merchantKeys.detail(id), 'reserves'] as const,
};

export function useMerchant(
  api: MerchantsApi,
  merchantId: string,
  options?: Omit<UseQueryOptions<Merchant>, 'queryKey' | 'queryFn'>
) {
  return useQuery({
    queryKey: merchantKeys.detail(merchantId),
    queryFn: () => api.getMerchant(merchantId),
    ...options,
  });
}

export function useMerchants(
  api: MerchantsApi,
  params?: ListMerchantsParams,
  options?: Omit<UseQueryOptions<Merchant[]>, 'queryKey' | 'queryFn'>
) {
  return useQuery({
    queryKey: merchantKeys.list(params),
    queryFn: () => api.listMerchants(params || {}),
    ...options,
  });
}

export function useMerchantBalances(
  api: MerchantsApi,
  merchantId: string,
  options?: Omit<UseQueryOptions<MerchantBalances>, 'queryKey' | 'queryFn'>
) {
  return useQuery({
    queryKey: merchantKeys.balances(merchantId),
    queryFn: () => api.getMerchantBalances(merchantId),
    ...options,
  });
}

export function useOnboardMerchant(
  api: MerchantsApi,
  options?: UseMutationOptions<Merchant, Error, OnboardMerchantRequest>
) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (data: OnboardMerchantRequest) => api.onboardMerchant(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: merchantKeys.lists() });
    },
    ...options,
  });
}
