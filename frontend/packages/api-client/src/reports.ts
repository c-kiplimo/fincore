import type { AxiosInstance } from 'axios';
import type { Report, GenerateReportRequest } from '@fincore/types';

export interface ListReportsParams {
  page?: number;
  pageSize?: number;
  type?: string;
  status?: string;
}

/**
 * Reports API Client
 */
export class ReportsApi {
  constructor(private client: AxiosInstance) {}

  async generateReport(data: GenerateReportRequest): Promise<Report> {
    const response = await this.client.post<Report>('/v1/reports', { payload: data });
    return response.data;
  }

  async getReport(reportId: string): Promise<Report> {
    const response = await this.client.get<Report>(`/v1/reports/${reportId}`);
    return response.data;
  }

  async listReports(params: ListReportsParams): Promise<Report[]> {
    const response = await this.client.get<Report[]>('/v1/reports', { params });
    return response.data;
  }

  async downloadReport(reportId: string): Promise<Blob> {
    const response = await this.client.get(`/v1/reports/${reportId}/download`, {
      responseType: 'blob',
    });
    return response.data;
  }

  async deleteReport(reportId: string): Promise<void> {
    await this.client.delete(`/v1/reports/${reportId}`);
  }
}
