import { useQuery, useMutation, useQueryClient, type UseQueryOptions, type UseMutationOptions } from '@tanstack/react-query';
import type { Report, GenerateReportRequest } from '@fincore/types';
import type { ReportsApi, ListReportsParams } from '../reports';

export const reportKeys = {
  all: ['reports'] as const,
  lists: () => [...reportKeys.all, 'list'] as const,
  list: (params?: ListReportsParams) => [...reportKeys.lists(), params] as const,
  details: () => [...reportKeys.all, 'detail'] as const,
  detail: (id: string) => [...reportKeys.details(), id] as const,
};

export function useReport(
  api: ReportsApi,
  reportId: string,
  options?: Omit<UseQueryOptions<Report>, 'queryKey' | 'queryFn'>
) {
  return useQuery({
    queryKey: reportKeys.detail(reportId),
    queryFn: () => api.getReport(reportId),
    ...options,
  });
}

export function useReports(
  api: ReportsApi,
  params?: ListReportsParams,
  options?: Omit<UseQueryOptions<Report[]>, 'queryKey' | 'queryFn'>
) {
  return useQuery({
    queryKey: reportKeys.list(params),
    queryFn: () => api.listReports(params || {}),
    ...options,
  });
}

export function useGenerateReport(
  api: ReportsApi,
  options?: UseMutationOptions<Report, Error, GenerateReportRequest>
) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (data: GenerateReportRequest) => api.generateReport(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: reportKeys.lists() });
    },
    ...options,
  });
}

export function useDownloadReport(
  api: ReportsApi,
  options?: UseMutationOptions<Blob, Error, string>
) {
  return useMutation({
    mutationFn: (reportId: string) => api.downloadReport(reportId),
    ...options,
  });
}

export function useDeleteReport(
  api: ReportsApi,
  options?: UseMutationOptions<void, Error, string>
) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (reportId: string) => api.deleteReport(reportId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: reportKeys.lists() });
    },
    ...options,
  });
}
