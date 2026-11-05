/**
 * Report types
 */

export type ReportType = 'TRANSACTIONS' | 'PAYOUTS' | 'REVENUE' | 'TAX' | 'RECONCILIATION';
export type ReportFormat = 'CSV' | 'PDF' | 'EXCEL';
export type ReportStatus = 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED';

export interface Report {
  id: string;
  type: ReportType;
  format: ReportFormat;
  status: ReportStatus;
  startDate: string;
  endDate: string;
  fileUrl?: string;
  errorMessage?: string;
  createdAt: string;
  completedAt?: string;
}

export interface GenerateReportRequest {
  type: ReportType;
  format: ReportFormat;
  startDate: string;
  endDate: string;
  filters?: Record<string, any>;
}
