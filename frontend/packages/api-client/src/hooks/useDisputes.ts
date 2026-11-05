import { useQuery, useMutation, useQueryClient, type UseQueryOptions, type UseMutationOptions } from '@tanstack/react-query';
import type { Dispute, CreateDisputeRequest, ResolveDisputeRequest } from '@fincore/types';
import type { DisputesApi, ListDisputesParams } from '../disputes';

export const disputeKeys = {
  all: ['disputes'] as const,
  lists: () => [...disputeKeys.all, 'list'] as const,
  list: (params?: ListDisputesParams) => [...disputeKeys.lists(), params] as const,
  details: () => [...disputeKeys.all, 'detail'] as const,
  detail: (id: string) => [...disputeKeys.details(), id] as const,
};

export function useDispute(
  api: DisputesApi,
  disputeId: string,
  options?: Omit<UseQueryOptions<Dispute>, 'queryKey' | 'queryFn'>
) {
  return useQuery({
    queryKey: disputeKeys.detail(disputeId),
    queryFn: () => api.getDispute(disputeId),
    ...options,
  });
}

export function useDisputes(
  api: DisputesApi,
  params?: ListDisputesParams,
  options?: Omit<UseQueryOptions<Dispute[]>, 'queryKey' | 'queryFn'>
) {
  return useQuery({
    queryKey: disputeKeys.list(params),
    queryFn: () => api.listDisputes(params || {}),
    ...options,
  });
}

export function useOpenDispute(
  api: DisputesApi,
  options?: UseMutationOptions<Dispute, Error, { paymentIntentId: string; data: CreateDisputeRequest }>
) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ paymentIntentId, data }) => api.openDispute(paymentIntentId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: disputeKeys.lists() });
    },
    ...options,
  });
}

export function useResolveDispute(
  api: DisputesApi,
  options?: UseMutationOptions<Dispute, Error, { disputeId: string; data: ResolveDisputeRequest }>
) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ disputeId, data }) => api.resolveDispute(disputeId, data),
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: disputeKeys.detail(data.id) });
      queryClient.invalidateQueries({ queryKey: disputeKeys.lists() });
    },
    ...options,
  });
}

export function useUploadEvidence(
  api: DisputesApi,
  options?: UseMutationOptions<Dispute, Error, { disputeId: string; file: File; description?: string }>
) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ disputeId, file, description }) => api.uploadEvidence(disputeId, file, description),
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: disputeKeys.detail(data.id) });
    },
    ...options,
  });
}
