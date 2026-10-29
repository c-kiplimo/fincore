import { useQuery, useMutation, useQueryClient, type UseQueryOptions, type UseMutationOptions } from '@tanstack/react-query';
import type { PaymentIntent, CreatePaymentRequest, RefundRequest } from '@fincore/types';
import type { PaymentsApi, ListPaymentsParams } from '../payments';

/**
 * React Query hooks for Payments API
 */

export const paymentKeys = {
  all: ['payments'] as const,
  lists: () => [...paymentKeys.all, 'list'] as const,
  list: (params?: ListPaymentsParams) => [...paymentKeys.lists(), params] as const,
  details: () => [...paymentKeys.all, 'detail'] as const,
  detail: (id: string) => [...paymentKeys.details(), id] as const,
};

/**
 * Hook to fetch a single payment by ID
 */
export function usePayment(
  api: PaymentsApi,
  id: string,
  options?: Omit<UseQueryOptions<PaymentIntent>, 'queryKey' | 'queryFn'>
) {
  return useQuery({
    queryKey: paymentKeys.detail(id),
    queryFn: () => api.getPaymentIntent(id),
    ...options,
  });
}

/**
 * Hook to fetch list of payments
 */
export function usePayments(
  api: PaymentsApi,
  params?: ListPaymentsParams,
  options?: Omit<UseQueryOptions<any>, 'queryKey' | 'queryFn'>
) {
  return useQuery({
    queryKey: paymentKeys.list(params),
    queryFn: () => api.listPaymentIntents(params),
    ...options,
  });
}

/**
 * Hook to create a payment intent
 */
export function useCreatePayment(
  api: PaymentsApi,
  options?: UseMutationOptions<PaymentIntent, Error, CreatePaymentRequest>
) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (data: CreatePaymentRequest) => api.createPaymentIntent(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: paymentKeys.lists() });
    },
    ...options,
  });
}

/**
 * Hook to capture a payment
 */
export function useCapturePayment(
  api: PaymentsApi,
  options?: UseMutationOptions<PaymentIntent, Error, string>
) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (id: string) => api.capturePayment(id),
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: paymentKeys.detail(data.id) });
      queryClient.invalidateQueries({ queryKey: paymentKeys.lists() });
    },
    ...options,
  });
}

/**
 * Hook to refund a payment
 */
export function useRefundPayment(
  api: PaymentsApi,
  options?: UseMutationOptions<PaymentIntent, Error, { id: string; data?: RefundRequest }>
) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ id, data }) => api.refundPayment(id, data),
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: paymentKeys.detail(data.id) });
      queryClient.invalidateQueries({ queryKey: paymentKeys.lists() });
    },
    ...options,
  });
}

/**
 * Hook to cancel a payment
 */
export function useCancelPayment(
  api: PaymentsApi,
  options?: UseMutationOptions<PaymentIntent, Error, string>
) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (id: string) => api.cancelPayment(id),
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: paymentKeys.detail(data.id) });
      queryClient.invalidateQueries({ queryKey: paymentKeys.lists() });
    },
    ...options,
  });
}
