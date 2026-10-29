/**
 * Common shared types
 */

export interface PaginatedResponse<T> {
  data: T[];
  meta: {
    page: number;
    pageSize: number;
    total: number;
    totalPages: number;
  };
}

export interface ApiError {
  code: string;
  message: string;
  field?: string;
}

export interface ApiResponse<T> {
  data?: T;
  error?: ApiError;
  errors?: ApiError[];
}

export interface DashboardMetrics {
  todayRevenue: number;
  pendingSettlement: number;
  transactionCount: number;
  successRate: number;
  chartData: ChartDataPoint[];
}

export interface ChartDataPoint {
  date: string;
  value: number;
}
