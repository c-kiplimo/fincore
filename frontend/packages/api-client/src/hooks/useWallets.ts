import { useQuery, useMutation, useQueryClient, type UseQueryOptions, type UseMutationOptions } from '@tanstack/react-query';
import type { Wallet, Individual, Transfer, CreateWalletRequest, TopUpRequest, TransferRequest, RegisterIndividualRequest } from '@fincore/types';
import type { WalletsApi } from '../wallets';

export const walletKeys = {
  all: ['wallets'] as const,
  lists: () => [...walletKeys.all, 'list'] as const,
  list: (individualId?: string) => [...walletKeys.lists(), individualId] as const,
  details: () => [...walletKeys.all, 'detail'] as const,
  detail: (id: string) => [...walletKeys.details(), id] as const,
  transfers: (id: string) => [...walletKeys.detail(id), 'transfers'] as const,
};

export const individualKeys = {
  all: ['individuals'] as const,
  detail: (id: string) => [...individualKeys.all, id] as const,
};

export function useIndividual(
  api: WalletsApi,
  individualId: string,
  options?: Omit<UseQueryOptions<Individual>, 'queryKey' | 'queryFn'>
) {
  return useQuery({
    queryKey: individualKeys.detail(individualId),
    queryFn: () => api.getIndividual(individualId),
    ...options,
  });
}

export function useWallet(
  api: WalletsApi,
  walletId: string,
  options?: Omit<UseQueryOptions<Wallet>, 'queryKey' | 'queryFn'>
) {
  return useQuery({
    queryKey: walletKeys.detail(walletId),
    queryFn: () => api.getWallet(walletId),
    ...options,
  });
}

export function useWallets(
  api: WalletsApi,
  individualId: string,
  options?: Omit<UseQueryOptions<Wallet[]>, 'queryKey' | 'queryFn'>
) {
  return useQuery({
    queryKey: walletKeys.list(individualId),
    queryFn: () => api.listWallets(individualId),
    ...options,
  });
}

export function useTransferHistory(
  api: WalletsApi,
  walletId: string,
  options?: Omit<UseQueryOptions<Transfer[]>, 'queryKey' | 'queryFn'>
) {
  return useQuery({
    queryKey: walletKeys.transfers(walletId),
    queryFn: () => api.getTransferHistory(walletId),
    ...options,
  });
}

export function useRegisterIndividual(
  api: WalletsApi,
  options?: UseMutationOptions<Individual, Error, RegisterIndividualRequest>
) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (data: RegisterIndividualRequest) => api.registerIndividual(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: individualKeys.all });
    },
    ...options,
  });
}

export function useOpenWallet(
  api: WalletsApi,
  options?: UseMutationOptions<Wallet, Error, { individualId: string; data: CreateWalletRequest }>
) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ individualId, data }) => api.openWallet(individualId, data),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: walletKeys.list(variables.individualId) });
    },
    ...options,
  });
}

export function useTopUp(
  api: WalletsApi,
  options?: UseMutationOptions<Transfer, Error, { walletId: string; data: TopUpRequest }>
) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ walletId, data }) => api.topUp(walletId, data),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: walletKeys.detail(variables.walletId) });
      queryClient.invalidateQueries({ queryKey: walletKeys.transfers(variables.walletId) });
    },
    ...options,
  });
}

export function useTransfer(
  api: WalletsApi,
  options?: UseMutationOptions<Transfer, Error, TransferRequest>
) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (data: TransferRequest) => api.transfer(data),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: walletKeys.detail(variables.fromWalletId) });
      queryClient.invalidateQueries({ queryKey: walletKeys.detail(variables.toWalletId) });
      queryClient.invalidateQueries({ queryKey: walletKeys.transfers(variables.fromWalletId) });
      queryClient.invalidateQueries({ queryKey: walletKeys.transfers(variables.toWalletId) });
    },
    ...options,
  });
}
