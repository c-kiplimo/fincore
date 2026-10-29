'use client';

import { Card, CardContent, CardHeader, CardTitle, Badge, Button } from '@payments-platform/ui';

export function ReportHistory() {
  // TODO: Fetch from API
  const reports = [
    {
      id: 'rep_abc123',
      type: 'Transaction Report',
      format: 'CSV',
      dateRange: 'Oct 1 - Oct 29, 2026',
      status: 'completed',
      generatedAt: '2026-10-29T10:00:00Z',
      downloadUrl: '/reports/rep_abc123.csv',
    },
    {
      id: 'rep_def456',
      type: 'Revenue Summary',
      format: 'PDF',
      dateRange: 'Sep 1 - Sep 30, 2026',
      status: 'completed',
      generatedAt: '2026-10-01T09:00:00Z',
      downloadUrl: '/reports/rep_def456.pdf',
    },
  ];

  const handleDownload = (reportId: string) => {
    // TODO: Download report
    console.log('Download report:', reportId);
    alert('Downloading report...');
  };

  return (
    <Card>
      <CardHeader>
        <CardTitle>Report History</CardTitle>
      </CardHeader>
      <CardContent className="p-0">
        <div className="divide-y divide-gray-200">
          {reports.map((report) => (
            <div
              key={report.id}
              className="p-6 hover:bg-gray-50 transition-colors"
            >
              <div className="flex items-start justify-between">
                <div className="flex-1">
                  <div className="flex items-center gap-3">
                    <h3 className="font-semibold">{report.type}</h3>
                    <Badge variant="completed">{report.format}</Badge>
                    <Badge variant="completed">{report.status}</Badge>
                  </div>

                  <div className="mt-2 text-sm text-gray-600">
                    <p>Date Range: {report.dateRange}</p>
                    <p>Generated: {new Date(report.generatedAt).toLocaleString()}</p>
                  </div>
                </div>

                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => handleDownload(report.id)}
                >
                  Download
                </Button>
              </div>
            </div>
          ))}

          {reports.length === 0 && (
            <div className="text-center py-12">
              <p className="text-gray-500">No reports generated yet</p>
              <p className="text-sm text-gray-400 mt-1">
                Create your first report above
              </p>
            </div>
          )}
        </div>
      </CardContent>
    </Card>
  );
}
